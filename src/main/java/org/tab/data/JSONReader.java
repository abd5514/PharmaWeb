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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JSONReader {
    // Reuse ObjectMapper (thread-safe for read ops)
    private static final ObjectMapper MAPPER = new ObjectMapper(new JsonFactory());

    // Cache compiled paths like "displayName.text" or "types[0]" -> steps
    private static final Map<String, List<PathStep>> PATH_CACHE = new ConcurrentHashMap<>();

    private final JsonNode root;

    /* ---------------------- Constructors ---------------------- */

    /** Loads JSON from PropReader key "JSONFilePath" (or default path). */
    public JSONReader() throws IOException {
        this(PropReader.get("JSONFilePath", "src/test/resources/testData.json"));
    }

    /** Load from explicit file path. */
    public JSONReader(String filePath) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Path.of(filePath)))) {
            this.root = MAPPER.readTree(in);
        }
    }

    /** Load from already-parsed node (optional use). */
    public JSONReader(JsonNode node) {
        this.root = node;
    }

    /* ---------------------- Meta helpers ---------------------- */

    public boolean isArray()       { return root.isArray(); }
    public boolean isObject()      { return root.isObject(); }
    /** If array → number of elements; if object → 1 (treat as single item). */
    public int size()              { return isArray() ? root.size() : 1; }

    /* ---------------------- Core navigation ---------------------- */

    /** Get the node to operate on: if array, use index; if object, ignore index. */
    private JsonNode baseNode(Integer indexOrNull) {
        if (isArray()) {
            if (indexOrNull == null)
                throw new IllegalStateException("Root is an array; please provide an index.");
            if (indexOrNull < 0 || indexOrNull >= root.size())
                throw new IndexOutOfBoundsException("Index " + indexOrNull + " out of bounds (size=" + root.size() + ")");
            return root.get(indexOrNull);
        }
        return root; // object root
    }

    /** Navigate using a compiled dot path with optional [index] steps. */
    private static JsonNode navigate(JsonNode start, String path) {
        if (path == null || path.isEmpty()) return start;
        List<PathStep> steps = PATH_CACHE.computeIfAbsent(path, JSONReader::compilePath);
        JsonNode current = start;
        for (PathStep step : steps) {
            if (current == null || current.isMissingNode()) return current;
            if (!step.field.isEmpty()) current = current.path(step.field);
            if (step.index != null) {
                if (!current.isArray() || step.index < 0 || step.index >= current.size()) {
                    // mirror old behavior: return missing node when out of range
                    return current.path(step.index == null ? 0 : step.index);
                }
                current = current.get(step.index);
            }
        }
        return current;
    }

    /** Pre-parse "a.b[2].c" into steps once. */
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
            out.add(new PathStep(field, idx));
        }
        return out;
    }

    private static final class PathStep {
        final String field;
        final Integer index; // nullable
        PathStep(String field, Integer index) {
            this.field = field == null ? "" : field;
            this.index = index;
        }
    }

    /* ---------------------- Get value from OBJECT or specific ARRAY item ---------------------- */

    public String  getString(String path)                 { return navigate(baseNode(null), path).asText(); }
    public int     getInt(String path)                    { return navigate(baseNode(null), path).asInt(); }
    public long    getLong(String path)                   { return navigate(baseNode(null), path).asLong(); }
    public double  getDouble(String path)                 { return navigate(baseNode(null), path).asDouble(); }
    public boolean getBoolean(String path)                { return navigate(baseNode(null), path).asBoolean(); }
    public JsonNode getNode(String path)                  { return navigate(baseNode(null), path); }

    public String  getString(int index, String path)      { return navigate(baseNode(index), path).asText(); }
    public int     getInt(int index, String path)         { return navigate(baseNode(index), path).asInt(); }
    public long    getLong(int index, String path)        { return navigate(baseNode(index), path).asLong(); }
    public double  getDouble(int index, String path)      { return navigate(baseNode(index), path).asDouble(); }
    public boolean getBoolean(int index, String path)     { return navigate(baseNode(index), path).asBoolean(); }
    public JsonNode getNode(int index, String path)       { return navigate(baseNode(index), path); }

    /* ---------------------- Bulk getters across ARRAY (or singleton list for OBJECT) ---------------------- */

    /** For array roots: map each item → value at path. For object root: returns singleton list. */
    public List<String>  getAllStrings(String path)  { return mapAll(path, JsonNode::asText); }
    public List<Integer> getAllInts(String path)     { return mapAll(path, JsonNode::asInt); }
    public List<Long>    getAllLongs(String path)    { return mapAll(path, JsonNode::asLong); }
    public List<Double>  getAllDoubles(String path)  { return mapAll(path, JsonNode::asDouble); }
    public List<Boolean> getAllBooleans(String path) { return mapAll(path, JsonNode::asBoolean); }

    private interface NodeMapper<T> { T map(JsonNode n); }

    private <T> List<T> mapAll(String path, NodeMapper<T> mapper) {
        List<T> out = new ArrayList<>();
        if (isArray()) {
            // plain for-loop keeps exact order/behavior
            List<PathStep> steps = PATH_CACHE.computeIfAbsent(path, JSONReader::compilePath);
            for (int i = 0; i < root.size(); i++) {
                JsonNode current = root.get(i);
                for (PathStep step : steps) {
                    if (current == null || current.isMissingNode()) break;
                    if (!step.field.isEmpty()) current = current.path(step.field);
                    if (step.index != null) {
                        if (!current.isArray() || step.index < 0 || step.index >= current.size()) {
                            current = current.path(step.index == null ? 0 : step.index);
                            break;
                        }
                        current = current.get(step.index);
                    }
                }
                out.add(mapper.map(current));
            }
        } else {
            out.add(mapper.map(navigate(root, path)));
        }
        return out;
    }
}