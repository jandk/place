package be.twofold.place;

import be.twofold.place.model.Placement;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Place {
    private static final ExecutorService ImageSaver = Executors.newFixedThreadPool(24);

    private static final List<Color> Colors = List.of(
        Color.decode("#FFFFFF"),
        Color.decode("#E4E4E4"),
        Color.decode("#888888"),
        Color.decode("#222222"),
        Color.decode("#FFA7D1"),
        Color.decode("#E50000"),
        Color.decode("#E59500"),
        Color.decode("#A06A42"),
        Color.decode("#E5D900"),
        Color.decode("#94E044"),
        Color.decode("#02BE01"),
        Color.decode("#00E5F0"),
        Color.decode("#0083C7"),
        Color.decode("#0000EA"),
        Color.decode("#E04AFF"),
        Color.decode("#820080")
    );

    public static void main(String[] args) {
        List<Placement> placements = readPlacements();

        BufferedImage image = new BufferedImage(1000, 1000, BufferedImage.TYPE_BYTE_INDEXED, colorModel());
        byte[] rawImage = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        int cutoff = 1490979600;
        for (Placement placement : placements) {
            if (placement.getTs() > cutoff) {
                dumpImage(image, cutoff);
                cutoff += 60;
            }

            int x = placement.getX();
            int y = placement.getY();
            if (x < 1000 && y < 1000) {
                int index = y * 1000 + x;
                rawImage[index] = (byte) placement.getColor();
            }
        }

        dumpImage(image, Integer.MAX_VALUE);
    }

    private static void dumpImage(BufferedImage image, int cutoff) {
        ColorModel colorModel = image.getColorModel();
        boolean alphaPremultiplied = colorModel.isAlphaPremultiplied();
        WritableRaster writableRaster = image.copyData(null);
        BufferedImage copy = new BufferedImage(colorModel, writableRaster, alphaPremultiplied, null);

        File file = new File("C:\\Temp\\place_" + Instant.ofEpochSecond(cutoff).toString().substring(0, 16).replace(':', '-') + ".png");
        ImageSaver.submit(() -> ImageIO.write(copy, "png", file));
    }

    private static IndexColorModel colorModel() {
        int size = Colors.size();
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            r[i] = (byte) Colors.get(i).getRed();
            g[i] = (byte) Colors.get(i).getGreen();
            b[i] = (byte) Colors.get(i).getBlue();
        }
        return new IndexColorModel(4, 16, r, g, b);
    }

    private static List<Placement> readPlacements() {
        try (Stream<String> lines = Files.lines(Paths.get("C:\\Temp\\place_tiles_sha1.csv"))) {
            return lines.map(Place::parsePlacement).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Placement parsePlacement(String s) {
        String[] split = s.split(",");
        long ts = Long.parseLong(split[0]);
        int x = Integer.parseInt(split[2]);
        int y = Integer.parseInt(split[3]);
        int color = Integer.parseInt(split[4]);
        return new Placement(ts, 0, x, y, color);
    }
}
