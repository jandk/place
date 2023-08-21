package be.twofold.place;

import be.twofold.place.model.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

final class Renderer {

    static final List<Color> Colors2017 = List.of(
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

    static final List<Color> Colors2022 = List.of(
        new Color(0xff, 0xff, 0xff),
        new Color(0xd4, 0xd7, 0xd9),
        new Color(0x89, 0x8d, 0x90),
        new Color(0x00, 0x00, 0x00),
        new Color(0x9c, 0x69, 0x26),
        new Color(0xff, 0x99, 0xaa),
        new Color(0xb4, 0x4a, 0xc0),
        new Color(0x81, 0x1e, 0x9f),
        new Color(0x51, 0xe9, 0xf4),
        new Color(0x36, 0x90, 0xea),
        new Color(0x24, 0x50, 0xa4),
        new Color(0x7e, 0xed, 0x56),
        new Color(0x00, 0xa3, 0x68),
        new Color(0xff, 0xd6, 0x35),
        new Color(0xff, 0xa8, 0x00),
        new Color(0xff, 0x45, 0x00),

        new Color(0x6d, 0x48, 0x2f),
        new Color(0xff, 0x38, 0x81),
        new Color(0x6a, 0x5c, 0xff),
        new Color(0x49, 0x3a, 0xc1),
        new Color(0x00, 0x9e, 0xaa),
        new Color(0x00, 0x75, 0x6f),
        new Color(0x00, 0xcc, 0x78),
        new Color(0xbe, 0x00, 0x39),

        new Color(0x51, 0x52, 0x52),
        new Color(0xff, 0xb4, 0x70),
        new Color(0xde, 0x10, 0x7f),
        new Color(0xe4, 0xab, 0xff),
        new Color(0x94, 0xb3, 0xff),
        new Color(0x00, 0xcc, 0xc0),
        new Color(0xff, 0xf8, 0xb8),
        new Color(0x6d, 0x00, 0x1a)
    );

    static final List<Color> Colors2023 = List.of(
        new Color(0xff, 0x45, 0x00),
        new Color(0xff, 0xa8, 0x00),
        new Color(0xff, 0xd6, 0x35),
        new Color(0x00, 0xa3, 0x68),
        new Color(0x36, 0x90, 0xea),
        new Color(0xb4, 0x4a, 0xc0),
        new Color(0x00, 0x00, 0x00),
        new Color(0xff, 0xff, 0xff),

        new Color(0x7e, 0xed, 0x56),
        new Color(0x24, 0x50, 0xa4),
        new Color(0x51, 0xe9, 0xf4),
        new Color(0x81, 0x1e, 0x9f),
        new Color(0xff, 0x99, 0xaa),
        new Color(0x9c, 0x69, 0x26),
        new Color(0x89, 0x8d, 0x90),
        new Color(0xd4, 0xd7, 0xd9),

        new Color(0xbe, 0x00, 0x39),
        new Color(0x00, 0xcc, 0x78),
        new Color(0x00, 0x75, 0x6f),
        new Color(0x00, 0x9e, 0xaa),
        new Color(0x49, 0x3a, 0xc1),
        new Color(0x6a, 0x5c, 0xff),
        new Color(0xff, 0x38, 0x81),
        new Color(0x6d, 0x48, 0x2f),

        new Color(0x6d, 0x00, 0x1a),
        new Color(0xff, 0xf8, 0xb8),
        new Color(0x00, 0xcc, 0xc0),
        new Color(0x94, 0xb3, 0xff),
        new Color(0xe4, 0xab, 0xff),
        new Color(0xde, 0x10, 0x7f),
        new Color(0xff, 0xb4, 0x70),
        new Color(0x51, 0x52, 0x52)
    );

    private final Path placementsPath;
    private final Path targetDirectory;
    private final List<Color> colors;

    // Render variables
    private static final int FramePerMillis = 300 * 1000; // In milliseconds
    private final ExecutorService pool;
    private BufferedImage image;
    private byte[] imageBuffer;
    private int state = 0;
    private long cutoff;

    Renderer(Path placementsPath, Path targetDirectory, Year year) {
        this.placementsPath = Objects.requireNonNull(placementsPath);
        this.targetDirectory = Objects.requireNonNull(targetDirectory);
        this.colors = year.getValue() == 2017 ? Colors2017 : Colors2022;

        int processors = Runtime.getRuntime().availableProcessors() / 2;
        LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(processors * 2);
        pool = new ThreadPoolExecutor(processors, processors, 1, TimeUnit.MINUTES, workQueue);

        nextState();
    }

    void render() throws IOException {
        try (Stream<String> lines = Files.lines(placementsPath)) {
            lines
                .map(Placement::parse)
                .forEach(this::placePixel);
        }

        try {
            pool.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void placePixel(Placement placement) {
        if (cutoff == 0) {
            cutoff = (placement.getTimestamp() / FramePerMillis) * FramePerMillis;
        }

        if (placement.getTimestamp() > cutoff) {
            dumpImage(image, cutoff);
            cutoff += FramePerMillis;
        }

        int x = placement.getX();
        int y = placement.getY();
        if (x > image.getWidth() || y > image.getHeight()) {
            nextState();
        }

        int index = y * image.getWidth() + x;
        imageBuffer[index] = (byte) placement.getColor();
    }

    private void dumpImage(BufferedImage image, long cutoff) {
        ColorModel colorModel = image.getColorModel();
        WritableRaster writableRaster = image.copyData(null);
        BufferedImage copy = new BufferedImage(colorModel, writableRaster, colorModel.isAlphaPremultiplied(), null);

        String formattedDate = Instant.ofEpochMilli(cutoff).toString().replaceAll("[:-]", "");
        System.out.println("Dumping image: " + state + "\\" + formattedDate);
        File file = targetDirectory
            .resolve("place_" + state + "_" + formattedDate + ".png")
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
        state++;

        int width = state > 1 ? 2000 : 1000;
        int height = state > 2 ? 2000 : 1000;
        int paletteSize = (state + 1) * 8;

        BufferedImage oldImage = image;
        IndexColorModel colorModel = Utils.fromColors(colors.subList(0, paletteSize));
        image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        imageBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        if (oldImage != null) {
            image.getGraphics().drawImage(oldImage, 0, 0, null);
        }
    }

}
