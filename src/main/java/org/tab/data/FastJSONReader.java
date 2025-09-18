package org.tab.data;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tab.utils.PropReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FastJSONReader {
    // Reuse ObjectMapper (thread-safe for read ops)
    private static final ObjectMapper MAPPER = new ObjectMapper(new JsonFactory());

    // Cache compiled paths like "displayName.text" or "types[0]" -> steps
    private static final Map<String, List<PathStep>> PATH_CACHE = new ConcurrentHashMap<>();

    private final JsonNode root;

    /** Loads JSON from PropReader key "JSONFilePath" (or default path). */
    public FastJSONReader() throws IOException {
        this(PropReader.get("JSONFilePath", "src/test/resources/Tabuk_details_json_chunk_1.json"));
    }

    /** Load from explicit file path. */
    public FastJSONReader(String filePath) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of(filePath)))) {
            this.root = MAPPER.readTree(in);
        }
    }

    /** If array → number of elements; if object → 1 (treat as single item). */
    public int size() { return root.isArray() ? root.size() : 1; }

    /** Get node at array index (or root if object), optionally navigating a path. */
    public JsonNode getNode(int index, String path) { return navigate(baseNode(index), path); }

    public List<Item> toItems() {
        // Minimal allocations; matches your current usage:
        // url: googleMapsUri , name: displayName.text  :contentReference[oaicite:3]{index=3}
        List<Item> items = new ArrayList<>(size());
        if (root.isArray()) {
            for (int i = 0; i < root.size(); i++) {
                JsonNode n = root.get(i);
                String url = injectLangAfterCid(n.path("googleMapsUri").asText());
                String name = n.path("displayName").path("text").asText();
                if (url != null && !url.isBlank() && name != null && !name.isBlank()) {
                    items.add(new Item(i, name, url));
                }
            }
        } else {
            String url = injectLangAfterCid(root.path("googleMapsUri").asText());
            String name = root.path("displayName").path("text").asText();
            if (url != null && !url.isBlank() && name != null && !name.isBlank()) {
                items.add(new Item(0, name, url));
            }
        }
        return items;
    }

    /* ---------------- internal path helpers (as in your current file) ---------------- */

    private JsonNode baseNode(Integer indexOrNull) {
        if (root.isArray()) {
            if (indexOrNull == null)
                throw new IllegalStateException("Root is an array; please provide an index.");
            if (indexOrNull < 0 || indexOrNull >= root.size())
                throw new IndexOutOfBoundsException("Index " + indexOrNull + " out of bounds (size=" + root.size() + ")");
            return root.get(indexOrNull);
        }
        return root;
    }

    private static JsonNode navigate(JsonNode start, String path) {
        if (path == null || path.isEmpty()) return start;
        List<PathStep> steps = PATH_CACHE.computeIfAbsent(path, FastJSONReader::compilePath);
        JsonNode current = start;
        for (PathStep step : steps) {
            if (current == null || current.isMissingNode()) return current;
            if (!step.field.isEmpty()) current = current.path(step.field);
            if (step.index != null) {
                if (!current.isArray() || step.index < 0 || step.index >= current.size()) {
                    return current.path(step.index == null ? 0 : step.index);
                }
                current = current.get(step.index);
            }
        }
        return current;
    }

    private static List<PathStep> compilePath(String path) {
        String[] parts = path.split("\\.");
        List<PathStep> out = new ArrayList<>(parts.length);
        for (String raw : parts) {
            String field = raw;
            Integer idx = null;
            int b = raw.indexOf('[');
            if (b != -1 && raw.endsWith("]")) {
                field = raw.substring(0, b);
                String inside = raw.substring(b + 1, raw.length() - 1).trim();
                if (!inside.isEmpty()) {
                    try { idx = Integer.parseInt(inside); } catch (NumberFormatException ignored) {}
                }
            }
            out.add(new PathStep(field == null ? "" : field, idx));
        }
        return out;
    }

    private static final class PathStep {
        final String field; final Integer index;
        PathStep(String field, Integer index) { this.field = field; this.index = index; }
    }

    /** Lightweight place item for parallel processing */
    public static final class Item {
        public final int index;
        public final String name;
        public final String url;
        public Item(int index, String name, String url) {
            this.index = index; this.name = name; this.url = url;
        }
    }

    public static String injectLangAfterCid(String url) {
        if (url == null || url.isEmpty()) return url;

        // Find "cid=" and the next "&"
        int cidIdx = url.indexOf("cid=");
        if (cidIdx != -1) {
            int ampIdx = url.indexOf("&", cidIdx);
            if (ampIdx != -1) {
                // Insert &hl=en&gl=us right before the "&"
                return url.substring(0, ampIdx)
                        + "&hl=en&gl=us"
                        + url.substring(ampIdx);
            }
        }

        // fallback: just append if no "&" found
        return url + "&hl=en&gl=us";
    }
}
