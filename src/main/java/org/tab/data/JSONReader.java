package org.tab.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tab.utils.PropReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JSONReader {
    private final JsonNode root;

    /* ---------------------- Constructors ---------------------- */

    /** Loads JSON from PropReader key "JSONFilePath" (or default path). */
    public JSONReader() throws IOException {
        this(PropReader.get("JSONFilePath", "src/test/resources/testData.json"));
    }

    /** Load from explicit file path. */
    public JSONReader(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.root = mapper.readTree(new File(filePath));
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

    /** Navigate using dot path, supporting simple bracket indexes: e.g. "displayName.text" or "types[0]". */
    private JsonNode navigate(JsonNode start, String path) {
        if (path == null || path.isEmpty()) return start;
        String[] parts = path.split("\\.");
        JsonNode current = start;
        for (String raw : parts) {
            if (current == null || current.isMissingNode()) return current;

            // Support field[index] in a single segment, e.g., "types[0]"
            String field = raw;
            Integer idx = null;

            int b = raw.indexOf('[');
            if (b != -1 && raw.endsWith("]")) {
                field = raw.substring(0, b);
                String inside = raw.substring(b + 1, raw.length() - 1);
                try { idx = Integer.parseInt(inside.trim()); } catch (NumberFormatException ignored) {}
            }

            if (!field.isEmpty()) current = current.path(field);
            if (idx != null) {
                if (!current.isArray() || idx < 0 || idx >= current.size()) return current.path(idx == null ? 0 : idx);
                current = current.get(idx);
            }
        }
        return current;
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
    public List<String> getAllStrings(String path)  { return mapAll(path, JsonNode::asText); }
    public List<Integer> getAllInts(String path)    { return mapAll(path, JsonNode::asInt); }
    public List<Long> getAllLongs(String path)      { return mapAll(path, JsonNode::asLong); }
    public List<Double> getAllDoubles(String path)  { return mapAll(path, JsonNode::asDouble); }
    public List<Boolean> getAllBooleans(String path){ return mapAll(path, JsonNode::asBoolean); }

    private interface NodeMapper<T> { T map(JsonNode n); }

    private <T> List<T> mapAll(String path, NodeMapper<T> mapper) {
        List<T> out = new ArrayList<>();
        if (isArray()) {
            for (int i = 0; i < root.size(); i++) {
                JsonNode n = navigate(root.get(i), path);
                out.add(mapper.map(n));
            }
        } else {
            out.add(mapper.map(navigate(root, path)));
        }
        return out;
    }
}