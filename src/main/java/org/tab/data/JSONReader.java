package org.tab.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tab.utils.PropReader;

import java.io.File;
import java.io.IOException;

public class JSONReader {
    private final JsonNode root;

    /**
     * Load JSON file once
     */
    public JSONReader() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        root = mapper.readTree(new File(PropReader.get("JSONFilePath", "src/test/resources/testData.json")));
    }

    /**
     * Get a string value by key
     */
    public String getString(String key) {
        return root.path(key).asText();
    }

    /**
     * Get an integer value by key
     */
    public int getInt(String key) {
        return root.path(key).asInt();
    }

    /**
     * Get a boolean value by key
     */
    public boolean getBoolean(String key) {
        return root.path(key).asBoolean();
    }

    /**
     * Get nested value with dot notation (e.g., "user.name")
     */
    public String getNested(String path) {
        String[] parts = path.split("\\.");
        JsonNode node = root;
        for (String part : parts) {
            node = node.path(part);
        }
        return node.asText();
    }
}

