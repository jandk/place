package be.twofold.place;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Place {

    // TODO: Something about 1490979600 as a cutoff for 2017 I need to check

    public static void main(String[] args) throws IOException {
        if (args.length < 3 || args.length > 4) {
            System.out.println("Usage: java -jar place.jar <year> <mode> <sourceDirectory> <targetDirectory>");
            System.out.println("  - year can be any of '2017', '2022'");
            System.out.println("  - mode can be any of 'simplify', 'render'");
            System.exit(1);
        }

        Year year = validateYear(args[0]);
        Properties properties = loadProperties(year);
        String mode = args[1];
        Path sourceDirectory = Path.of(args[2]);
        Path targetDirectory = args.length == 4 ? Path.of(args[3]) : sourceDirectory;
        Files.createDirectories(targetDirectory);

        if ("simplify".equals(mode)) {
            List<Path> sourceFiles = scanFiles(sourceDirectory, properties.getProperty("file_regex"));
            Simplifier simplifier = new Simplifier(sourceFiles, targetDirectory, year);
            simplifier.simplify();
        } else if ("render".equals(mode)) {
            Path placementsPath = sourceDirectory.resolve("placements.txt");
            new Renderer(placementsPath, targetDirectory, year).render();
        } else {
            System.out.println("Unknown mode: " + mode);
            System.exit(1);
        }
    }

    private static Year validateYear(String s) {
        Year year = Year.parse(s);
        if (year.getValue() != 2017 && year.getValue() != 2022) {
            throw new IllegalArgumentException("Year must be 2017 or 2022");
        }
        return year;
    }

    private static Properties loadProperties(Year year) throws IOException {
        try (InputStream in = Place.class.getResourceAsStream("/" + year + ".properties")) {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
    }

    private static List<Path> scanFiles(Path root, String regex) throws IOException {
        try (Stream<Path> list = Files.list(root)) {
            return list
                .filter(path -> path.getFileName().toString().matches(regex))
                .sorted()
                .collect(Collectors.toList());
        }
    }

}
