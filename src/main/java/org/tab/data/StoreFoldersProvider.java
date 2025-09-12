package org.tab.data;

import org.tab.utils.PropReader;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StoreFoldersProvider {
    private static final Path BASE = Path.of(PropReader.get("ImageFilesPath", "src/test/resources/images"));

    @DataProvider(name = "stores", parallel = true)
    public static Object[][] stores() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        if (!Files.isDirectory(BASE)) return new Object[0][];
        try (var dirs = Files.list(BASE)) {
            dirs.filter(Files::isDirectory).forEach(p -> {
                String store = storeFromFolder(p.getFileName().toString());
                rows.add(new Object[]{store, p.toFile()});
            });
        }
        return rows.toArray(new Object[0][]);
    }

    private static String storeFromFolder(String folderName) {
        // Default: folder name == store name (e.g., "Asif Cafè")
        // Adjust if you use a different naming convention.
        return folderName;
    }
}