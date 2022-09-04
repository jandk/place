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

public class Place2017 {
    private static final ExecutorService ImageSaver = Executors.newFixedThreadPool(24);

    private static final List<Color> Colors = List.of(
        new Color(0xff, 0xff, 0xff),
        new Color(0xe4, 0xe4, 0xe4),
        new Color(0x88, 0x88, 0x88),
        new Color(0x22, 0x22, 0x22),
        new Color(0xff, 0xa7, 0xd1),
        new Color(0xe5, 0x00, 0x00),
        new Color(0xe5, 0x95, 0x00),
        new Color(0xa0, 0x6a, 0x42),
        new Color(0xe5, 0xd9, 0x00),
        new Color(0x94, 0xe0, 0x44),
        new Color(0x02, 0xbe, 0x01),
        new Color(0x00, 0xe5, 0xf0),
        new Color(0x00, 0x83, 0xc7),
        new Color(0x00, 0x00, 0xea),
        new Color(0xe0, 0x4a, 0xff),
        new Color(0x82, 0x00, 0x80)
    );

    public static void main(String[] args) {
        List<Placement> placements = readPlacements();

        IndexColorModel colorModel = Utils.fromColors(Colors);
        BufferedImage image = new BufferedImage(1000, 1000, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        byte[] rawImage = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        int cutoff = 1490979600;
        for (Placement placement : placements) {
            if (placement.getTimestamp() > cutoff) {
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

    private static List<Placement> readPlacements() {
        try (Stream<String> lines = Files.lines(Paths.get("C:\\Temp\\place_tiles_sha1.csv"))) {
            return lines.map(Place2017::parsePlacement).collect(Collectors.toList());
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
