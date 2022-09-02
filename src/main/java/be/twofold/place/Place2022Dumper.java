package be.twofold.place;

import be.twofold.place.actions.PlacementDumper;
import be.twofold.place.actions.UserDumper;
import be.twofold.place.actions.UserReader;
import be.twofold.place.model.ByteArray;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Place2022Dumper {

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

}
