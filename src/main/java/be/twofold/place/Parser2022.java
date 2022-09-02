package be.twofold.place;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Parser2022 {

    public static void main(String[] args) throws IOException {
        System.out.println("Scanning for files...");
        List<Path> paths = scanForFiles();

    }

    private static List<Path> scanForFiles() throws IOException {
        try (Stream<Path> list = Files.list(Paths.get("C:\\Temp\\place2022"))) {
            return list
                .filter(path -> path.getFileName().toString().endsWith(".gzip"))
                .sorted()
                .collect(Collectors.toList());
        }
    }

}
