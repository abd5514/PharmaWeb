package org.tab.tests;

import org.tab.base.Base;
import org.tab.data.JSONReader;
import org.tab.data.RetryFailedDownloads;
import org.tab.utils.ExtentReport.ExtentTestListener;
import org.tab.web_pages.GoogleMapPage;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import org.tab.data.GoogleDriveUploader; // <-- the helper class you added
import com.google.api.services.drive.Drive;

import java.nio.file.*;
import java.time.LocalDate;

import java.io.IOException;

import static org.tab.utils.common.SharedMethods.staticWait;

@Listeners(ExtentTestListener.class)
public class GoogleDriveUploaderTest extends Base {
    private String saJsonPath;         // e.g., src/test/resources/keys/serviceaccount.json
    private String impersonateUser;    // optional: Workspace user email, or "" to skip
    private boolean cleanTarget;
    private String localFolder;        // e.g., "src/test/resources/images"

    @BeforeClass
    public void setupProps() {
        saJsonPath      = System.getProperty("saJsonFile", "src/test/resources/service-account.json");
        impersonateUser = System.getProperty("gdrive.sa.user", ""); // leave empty if not using DWD
        localFolder     = System.getProperty("gdrive.local", "src/test/resources/images");
        cleanTarget     = Boolean.parseBoolean(System.getProperty("gdrive.clean", "true")); // <— default ON

        // Basic validations
        Assert.assertTrue(Files.exists(Path.of(saJsonPath)), "serviceaccount.json not found: " + saJsonPath);
        Assert.assertTrue(Files.isDirectory(Path.of(localFolder)), "Local folder not found: " + localFolder);
    }

    @Test(description = "Uploads a local folder into 'Stores' folder in Google Drive")
    public void uploadToStoresFolder() throws Exception {
        // Build Drive client with service account
        Drive drive = GoogleDriveUploader.buildDriveWithServiceAccount(
                saJsonPath,
                impersonateUser == null || impersonateUser.isBlank() ? null : impersonateUser
        );

        // Upload into: My Drive / Stores
        GoogleDriveUploader.uploadFolder(
                drive,
                Path.of(localFolder),
                null,          // parentId = My Drive
                "jeddah menus", // will create/use "Stores" folder in My Drive
                cleanTarget      // true => wipe contents first
        );

        Assert.assertTrue(true, "Folder uploaded successfully.");
    }
}
