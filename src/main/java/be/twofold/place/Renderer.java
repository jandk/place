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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Renderer {
    private static final int TimeSlot = 5000;

    private final ExecutorService pool = new ThreadPoolExecutor(24, 24, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(48));
    private final List<Color> palette;
    private final Path destination;
    private BufferedImage image;
    private byte[] imageBuffer;
    private int state = 0;
    private long cutoff = (1648817050315L / TimeSlot) * TimeSlot;

    public Renderer(List<Color> palette, Path destination) throws IOException {
        this.palette = palette;
        this.destination = destination;
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }

        nextState();
    }

    public void accept(Placement placement) throws IOException {
        int x = placement.getX();
        int y = placement.getY();
        if (x > image.getWidth() || y > image.getHeight()) {
            nextState();
        }

        if (placement.getTs() > cutoff) {
            dumpImage(image, cutoff);
            cutoff += TimeSlot;
        }

        int index = y * image.getWidth() + x;
        imageBuffer[index] = (byte) placement.getColor();
    }

    public void finish() {
        try {
            pool.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void dumpImage(BufferedImage image, long cutoff) {
        ColorModel colorModel = image.getColorModel();
        boolean alphaPremultiplied = colorModel.isAlphaPremultiplied();
        WritableRaster writableRaster = image.copyData(null);
        BufferedImage copy = new BufferedImage(colorModel, writableRaster, alphaPremultiplied, null);

        String formattedDate = Instant.ofEpochMilli(cutoff).toString().replaceAll("[:-]", "");
        System.out.println("Dumping image: " + state + "\\" + formattedDate);
        File file = destination
            .resolve(String.valueOf(state))
            .resolve("place_" + formattedDate + ".png")
            .toFile();

        while (true) {
            try {
                pool.submit(() -> ImageIO.write(copy, "png", file));
                break;
            } catch (RejectedExecutionException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void nextState() throws IOException {
        state++;
        Files.createDirectory(destination.resolve(String.valueOf(state)));
        updateImage();
    }

    private void updateImage() {
        int width = state > 1 ? 2000 : 1000;
        int height = state > 2 ? 2000 : 1000;

        BufferedImage oldImage = image;
        image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, palette());
        imageBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        if (oldImage != null) {
            byte[] oldImageBuffer = ((DataBufferByte) oldImage.getRaster().getDataBuffer()).getData();
            int oldWidth = oldImage.getWidth();
            int oldHeight = oldImage.getHeight();
            for (int y = 0; y < oldHeight; y++) {
                System.arraycopy(oldImageBuffer, y * oldWidth, imageBuffer, y * width, oldWidth);
            }
        }
    }

    private IndexColorModel palette() {
        int paletteSize = (state + 1) * 8;

        byte[] r = new byte[paletteSize];
        byte[] g = new byte[paletteSize];
        byte[] b = new byte[paletteSize];

        for (int i = 0; i < paletteSize; i++) {
            r[i] = (byte) palette.get(i).getRed();
            g[i] = (byte) palette.get(i).getGreen();
            b[i] = (byte) palette.get(i).getBlue();
        }

        return new IndexColorModel(paletteSize > 16 ? 5 : 4, paletteSize, r, g, b);
    }

}