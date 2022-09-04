package be.twofold.place;

import be.twofold.place.model.Placement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Place2017 {
    private static final ExecutorService ImageSaver = Executors.newFixedThreadPool(24);

    public static void main(String[] args) {
        List<Placement> placements = List.of();

        IndexColorModel colorModel = Utils.fromColors(Renderer.Colors2017);
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

}
