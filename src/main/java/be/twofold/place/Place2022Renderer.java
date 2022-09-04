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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class Place2022Renderer {
    static final List<Color> Colors = List.of(
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
    private static final int TimeSlot = 5000;
    private static final Path Root = Paths.get("C:\\Temp\\place2022");

    private final ExecutorService pool = new ThreadPoolExecutor(24, 24, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(48));
    private final List<Color> palette;
    private final Path destination;
    private BufferedImage image;
    private byte[] imageBuffer;
    private int state = 0;
    private long cutoff = (1648817050315L / TimeSlot) * TimeSlot;

    public Place2022Renderer(List<Color> palette, Path destination) throws IOException {
        this.palette = palette;
        this.destination = destination;
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }

        nextState();
    }

    public static void main(String[] args) throws IOException {
        Path framesPath = Root.resolve("frames");
        Place2022Renderer renderer = new Place2022Renderer(Colors, framesPath);

        try (Stream<String> lines = Files.lines(Root.resolve("placements.txt"))) {
            lines
                .map(Placement::parse)
                .forEach(renderer::accept);
        }
    }

    public void accept(Placement placement) {
        int x = placement.getX();
        int y = placement.getY();
        if (x > image.getWidth() || y > image.getHeight()) {
            nextState();
        }

        if (placement.getTimestamp() > cutoff) {
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

    private void nextState() {
        try {
            state++;
            Files.createDirectory(destination.resolve(String.valueOf(state)));
            updateImage();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void updateImage() {
        int width = state > 1 ? 2000 : 1000;
        int height = state > 2 ? 2000 : 1000;
        int paletteSize = (state + 1) * 8;

        BufferedImage oldImage = image;
        IndexColorModel colorModel = Utils.fromColors(palette.subList(0, paletteSize));
        image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
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

}
