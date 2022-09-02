package be.twofold.place;

import be.twofold.place.actions.PlacementDumper;
import be.twofold.place.actions.UserDumper;
import be.twofold.place.actions.UserReader;
import be.twofold.place.model.ByteArray;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Place2022Dumper {
    private static final List<Color> Colors = List.of(
        new Color(0xFF, 0xFF, 0xFF),
        new Color(0xD4, 0xD7, 0xD9),
        new Color(0x89, 0x8D, 0x90),
        new Color(0x00, 0x00, 0x00),
        new Color(0x9C, 0x69, 0x26),
        new Color(0xFF, 0x99, 0xAA),
        new Color(0xB4, 0x4A, 0xC0),
        new Color(0x81, 0x1E, 0x9F),
        new Color(0x51, 0xE9, 0xF4),
        new Color(0x36, 0x90, 0xEA),
        new Color(0x24, 0x50, 0xA4),
        new Color(0x7E, 0xED, 0x56),
        new Color(0x00, 0xA3, 0x68),
        new Color(0xFF, 0xD6, 0x35),
        new Color(0xFF, 0xA8, 0x00),
        new Color(0xFF, 0x45, 0x00),

        new Color(0x6D, 0x48, 0x2F),
        new Color(0xFF, 0x38, 0x81),
        new Color(0x6A, 0x5C, 0xFF),
        new Color(0x49, 0x3A, 0xC1),
        new Color(0x00, 0x9E, 0xAA),
        new Color(0x00, 0x75, 0x6F),
        new Color(0x00, 0xCC, 0x78),
        new Color(0xBE, 0x00, 0x39),

        new Color(0x51, 0x52, 0x52),
        new Color(0xFF, 0xB4, 0x70),
        new Color(0xDE, 0x10, 0x7F),
        new Color(0xE4, 0xAB, 0xFF),
        new Color(0x94, 0xB3, 0xFF),
        new Color(0x00, 0xCC, 0xC0),
        new Color(0xFF, 0xF8, 0xB8),
        new Color(0x6D, 0x00, 0x1A)
    );


    private static final Path Root = Paths.get("C:\\Temp\\place2022");

    public static void main(String[] args) {
        Path usersPath = Root.resolve("users.txt");
        Path placementsPath = Root.resolve("placements.txt");
        Path modsPath = Root.resolve("mods.txt");

        FileProcessor processor = new FileProcessor(Root);

        // Dump all the users in a separate file
        if (!Files.exists(usersPath)) {
            System.out.println("Dumping users");
            processor.process(new UserDumper(usersPath));
        }

        // Read all the users back in
        System.out.println("Reading users");
        Map<ByteArray, Integer> users = new UserReader(usersPath).get();

        // Dump all sorted placements
        System.out.println("Dumping sorted placements");
        PlacementDumper placementDumper = new PlacementDumper(placementsPath, users);
        processor.process(placementDumper);

        // Dump all mods
        System.out.println("Dumping mods");
        placementDumper.dumpMods(modsPath);
    }

//    private static void renderFrames() throws IOException {
//        Path framesPath = Root.resolve("frames");
//        Renderer renderer = new Renderer(Colors, framesPath);
//
//        Files.lines(Root.resolve("placements.txt"))
//            .map(Place2022::parsePlacement)
//            .forEach(renderer::accept);
//    }
//
//    private static Placement parsePlacement(String s) {
//        int i1 = s.indexOf(',');
//        int i2 = s.indexOf(',', i1 + 1);
//        int i3 = s.indexOf(',', i2 + 1);
//        int i4 = s.indexOf(',', i3 + 1);
//
//        return new Placement(
//            Long.parseLong(s, 0, i1, 10),
//            Integer.parseInt(s, i1 + 1, i2, 10),
//            Integer.parseInt(s, i2 + 1, i3, 10),
//            Integer.parseInt(s, i3 + 1, i4, 10),
//            Integer.parseInt(s, i4 + 1, s.length(), 10)
//        );
//    }

}
