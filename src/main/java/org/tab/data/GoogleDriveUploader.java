package org.tab.data;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Create;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.services.drive.model.File;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;

import java.io.*;
/*import java.io.File;*/
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GoogleDriveUploader {

    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_RETRIES = 6;               // ~ up to ~1+2+4+8+16+32s backoff

    /* =======================
       AUTH BUILDERS
       ======================= */

    /** Build Drive client using a Service Account key file (.json). Optionally impersonate a user (domain-wide delegation). */
    public static Drive buildDriveWithServiceAccount(String serviceAccountJsonPath, String userToImpersonate)
            throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credentials base = ServiceAccountCredentials
                .fromStream(new FileInputStream(serviceAccountJsonPath))
                .createScoped(SCOPES);

        if (userToImpersonate != null && !userToImpersonate.isBlank()) {
            base = ((ServiceAccountCredentials) base).createDelegated(userToImpersonate);
        }

        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(base))
                .setApplicationName("DriveUploader")
                .build();
    }

    /** Build Drive client using OAuth (Installed App). Stores tokens under tokensDir. */
    public static Drive buildDriveWithOAuth(String credentialsJsonPath, String tokensDir)
            throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        try (InputStream in = new FileInputStream(credentialsJsonPath)) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDir)))
                    .setAccessType("offline")
                    .build();

            var receiver = new com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver.Builder()
                    .setPort(8888)
                    .build();

            var credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName("DriveUploader")
                    .build();
        }
    }

    /* =======================
       PUBLIC API
       ======================= */

    /**
     * Upload a local folder recursively to a target Drive folder.
     * - If driveParentFolderId is null/blank, uploads to My Drive root.
     * - If ensurePath != null, it creates/uses My Drive:/ensurePath under the parent.
     */
    public static void uploadFolder(Drive drive,
                                    Path localRoot,
                                    String driveParentFolderId,
                                    String ensurePath) throws IOException {

        if (!Files.isDirectory(localRoot)) {
            throw new IllegalArgumentException("Local path is not a directory: " + localRoot);
        }

        String parentId = (driveParentFolderId == null || driveParentFolderId.isBlank())
                ? getMyDriveRootId(drive)
                : driveParentFolderId;

        if (ensurePath != null && !ensurePath.isBlank()) {
            parentId = ensureFolderPath(drive, parentId, ensurePath);
        }

        // Mirror the local tree
        uploadDirectoryRecursive(drive, localRoot, parentId);
    }

    /** Upload with option to clean the target path before mirroring. */
    public static void uploadFolder(Drive drive,
                                    Path localRoot,
                                    String driveParentFolderId,
                                    String ensurePath,
                                    boolean cleanTargetBeforeUpload) throws IOException {

        if (!Files.isDirectory(localRoot)) {
            throw new IllegalArgumentException("Local path is not a directory: " + localRoot);
        }

        String parentId = (driveParentFolderId == null || driveParentFolderId.isBlank())
                ? getMyDriveRootId(drive)
                : driveParentFolderId;

        // Ensure/locate the Stores path
        String targetId = (ensurePath != null && !ensurePath.isBlank())
                ? ensureFolderPath(drive, parentId, ensurePath)
                : parentId;

        // Clean the target folder contents if requested
        if (cleanTargetBeforeUpload) {
            emptyFolder(drive, targetId);
        }

        // Mirror the local tree into target
        uploadDirectoryRecursive(drive, localRoot, targetId);
    }


    /** Get (or cache) the user's root "My Drive" folder id. */
    private static String getMyDriveRootId(Drive drive) {
        // In Drive v3, the root folder is always referenced as "root"
        return "root";
    }

    /* =======================
       RECURSIVE UPLOAD
       ======================= */

    private static void uploadDirectoryRecursive(Drive drive, Path localDir, String driveParentId) throws IOException {
        // Ensure this directory exists in Drive
        String thisFolderId = findOrCreateFolder(drive, localDir.getFileName().toString(), driveParentId);

        try (var stream = Files.newDirectoryStream(localDir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    uploadDirectoryRecursive(drive, p, thisFolderId);
                } else if (Files.isRegularFile(p)) {
                    uploadFileIfNeeded(drive, p, thisFolderId);
                }
            }
        }
    }

    /* =======================
       FOLDER HELPERS
       ======================= */

    // --- CLEANUP HELPERS ---

    /** Delete everything inside a Drive folder (keeps the folder itself). */
    public static void emptyFolder(Drive drive, String folderId) throws IOException {
        String pageToken = null;
        do {
            var list = drive.files().list()
                    .setQ("'" + folderId + "' in parents and trashed = false")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, mimeType)")
                    .setSupportsAllDrives(true)
                    .setIncludeItemsFromAllDrives(true)
                    .setPageToken(pageToken)
                    .execute();

            if (list.getFiles() != null) {
                for (var f : list.getFiles()) {
                    // If it's a folder, delete its contents first (recursive), then delete the folder
                    if ("application/vnd.google-apps.folder".equals(f.getMimeType())) {
                        deleteFolderRecursive(drive, f.getId());
                    } else {
                        executeWithRetry(() -> {
                            drive.files().delete(f.getId())
                                    .setSupportsAllDrives(true)
                                    .execute();
                            return null;
                        });
                    }
                }
            }
            pageToken = list.getNextPageToken();
        } while (pageToken != null);
    }

    /** Recursively delete a Drive folder and all its children. */
    public static void deleteFolderRecursive(Drive drive, String folderId) throws IOException {
        emptyFolder(drive, folderId); // remove children first
        executeWithRetry(() -> {
            drive.files().delete(folderId)
                    .setSupportsAllDrives(true)
                    .execute();
            return null;
        });
    }


    /** Ensure nested path like "A/B/C" exists under parentId; return the deepest folder id. */
    public static String ensureFolderPath(Drive drive, String parentId, String path) throws IOException {
        String currentParent = parentId;
        for (String part : path.replace("\\", "/").split("/")) {
            if (part == null || part.isBlank()) continue;
            currentParent = findOrCreateFolder(drive, part.trim(), currentParent);
        }
        return currentParent;
    }

    /** Find a folder by name under parent; create if missing. */
    public static String findOrCreateFolder(Drive drive, String name, String parentId) throws IOException {
        String existingId = findSingleByName(drive, name, parentId, true);
        if (existingId != null) return existingId;

        File folderMeta = new File()
                .setName(name)
                .setMimeType("application/vnd.google-apps.folder")
                .setParents(Collections.singletonList(parentId));

        return executeWithRetry(() -> drive.files().create(folderMeta)
                .setFields("id")
                .execute()).getId();
    }

    /* =======================
       FILE HELPERS
       ======================= */

    /** Uploads file if not present; if present with same name in same folder, replaces content (new version). */
    private static void uploadFileIfNeeded(Drive drive, Path localFile, String driveParentId) throws IOException {
        String name = localFile.getFileName().toString();
        String existingId = findSingleByName(drive, name, driveParentId, false);

        String mime = probeMime(localFile);
        DateTime modified = new DateTime(Files.getLastModifiedTime(localFile).toMillis());

        File meta = new File()
                .setName(name)
                .setParents(Collections.singletonList(driveParentId))
                .setModifiedTime(modified);

        AbstractInputStreamContent content = new FileContent(mime, localFile.toFile());

        if (existingId == null) {
            // Create (resumable)
            Create create = drive.files().create(meta, content)
                    .setFields("id, name, parents, mimeType, modifiedTime");
            MediaHttpUploader uploader = create.getMediaHttpUploader();
            uploader.setChunkSize(CHUNK_SIZE);
            uploader.setDirectUploadEnabled(false);
            uploader.setProgressListener(defaultProgress(name));

            File created = executeWithRetry(create::execute);
            // Update modified time (metadata isn't guaranteed to persist with upload across all file types)
            patchModifiedTime(drive, created.getId(), modified);
            System.out.println("Uploaded: " + localFile + " -> " + created.getId());
        } else {
            // Update existing (new content)
            Drive.Files.Update update = drive.files().update(existingId, meta, content)
                    .setFields("id, name, parents, mimeType, modifiedTime");
            MediaHttpUploader uploader = update.getMediaHttpUploader();
            uploader.setChunkSize(CHUNK_SIZE);
            uploader.setDirectUploadEnabled(false);
            uploader.setProgressListener(defaultProgress(name));

            File updated = executeWithRetry(update::execute);
            patchModifiedTime(drive, updated.getId(), modified);
            System.out.println("Updated: " + localFile + " -> " + updated.getId());
        }
    }

    private static void patchModifiedTime(Drive drive, String fileId, DateTime modified) throws IOException {
        File patch = new File().setModifiedTime(modified);
        executeWithRetry(() -> drive.files().update(fileId, patch).setFields("id, modifiedTime").execute());
    }

    /** Find a single file/folder by name in parent. If folderOnly, restrict mimeType to folder. */
    // Escape single quotes for Drive search queries
    private static String escapeForDriveQuery(String s) {
        return s == null ? "" : s.replace("'", "\\'");
    }

    private static String findSingleByName(Drive drive, String name, String parentId, boolean folderOnly) throws IOException {
        String safeName = escapeForDriveQuery(name);

        // Build query: exact name match within parent, not trashed (+ optional folder mimeType)
        StringBuilder q = new StringBuilder();
        q.append("name = '").append(safeName).append("' ")
                .append("and '").append(parentId).append("' in parents ")
                .append("and trashed = false");
        if (folderOnly) {
            q.append(" and mimeType = 'application/vnd.google-apps.folder'");
        }

        FileList list = executeWithRetry(() -> drive.files().list()
                .setQ(q.toString())
                .setSpaces("drive")
                .setFields("files(id,name)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(10)
                .setSupportsAllDrives(true)          // for Shared Drives compatibility
                .setIncludeItemsFromAllDrives(true)  // include Shared Drive items
                .setCorpora("user,drive")            // search My Drive + any Shared Drives you have access to
                .execute());

        if (list.getFiles() == null || list.getFiles().isEmpty()) return null;
        return list.getFiles().get(0).getId();
    }


    private static String probeMime(Path file) {
        try {
            String type = Files.probeContentType(file);
            return (type != null) ? type : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private static MediaHttpUploaderProgressListener defaultProgress(String name) {
        return uploader -> {
            switch (uploader.getUploadState()) {
                case INITIATION_STARTED -> System.out.println("[INIT] " + name);
                case INITIATION_COMPLETE -> System.out.println("[START] " + name);
                case MEDIA_IN_PROGRESS -> System.out.printf("[PROG] %s %.2f%%%n", name, uploader.getProgress() * 100);
                case MEDIA_COMPLETE -> System.out.println("[DONE] " + name);
                default -> {}
            }
        };
    }

    /* =======================
       RETRY WRAPPER
       ======================= */

    @FunctionalInterface
    private interface IoCallable<T> { T call() throws IOException; }

    private static <T> T executeWithRetry(IoCallable<T> action) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.call();
            } catch (GoogleJsonResponseException gjre) {
                int code = gjre.getStatusCode();
                // Retry on 429 / 5xx
                if (code == 429 || (code >= 500 && code < 600)) {
                    last = gjre;
                } else {
                    throw gjre;
                }
            } catch (IOException ioe) {
                last = ioe; // network hiccups etc.
            }

            long sleep = (long) Math.min(32000, Math.pow(2, attempt) * 1000L);
            try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
        }
        throw last;
    }

    /* =======================
       EXAMPLE MAIN
       ======================= */

    /**
     * Example CLI:
     * 1) Service Account:
     *    java ... GoogleDriveUploader sa /path/key.json "" "MyApp/Backups" "C:/data/to-upload"
     *    (impersonate: put user email instead of "")
     *
     * 2) OAuth:
     *    java ... GoogleDriveUploader oauth /path/client_secret.json /path/tokens "MyApp/Backups" "C:/data/to-upload"
     *
     * Arguments:
     *  - mode: "sa" or "oauth"
     *  - cred1: path to serviceAccount.json (sa) OR client_secret.json (oauth)
     *  - cred2: (sa) user-to-impersonate or ""  |  (oauth) tokensDir
     *  - driveSubPath: Drive path to ensure under My Drive (e.g., "Backups/Run-2025-09-11")
     *  - localFolder: local directory to upload
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("""
                    Usage:
                      SA   : sa <serviceAccountJson> <userToImpersonate or ''> <driveSubPath> <localFolder>
                      OAuth: oauth <client_secret.json> <tokensDir> <driveSubPath> <localFolder>
                    """);
            return;
        }

        String mode = args[0];
        String cred1 = args[1];
        String cred2 = args[2];
        String driveSubPath = args[3];
        Path local = Paths.get(args[4]);

        Drive drive;
        if ("sa".equalsIgnoreCase(mode)) {
            String user = cred2.isBlank() ? null : cred2;
            drive = buildDriveWithServiceAccount(cred1, user);
        } else if ("oauth".equalsIgnoreCase(mode)) {
            drive = buildDriveWithOAuth(cred1, cred2);
        } else {
            throw new IllegalArgumentException("mode must be 'sa' or 'oauth'");
        }

        uploadFolder(drive, local, null, driveSubPath);
        System.out.println("✅ Upload complete.");
    }
}
