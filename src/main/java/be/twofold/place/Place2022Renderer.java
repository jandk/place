package be.twofold.place;

import be.twofold.place.model.Placement;

import javax.imageio.ImageIO;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class Place2022Renderer {
    private static final int TimeSlot = 5000;
    private static final Path Root = Paths.get("C:\\Temp\\place2022");

    private final ExecutorService pool;
    private final Path destination;
    private BufferedImage image;
    private byte[] imageBuffer;
    private int state = 0;
    private long cutoff = (1648817050315L / TimeSlot) * TimeSlot;

    static {
    }

    public Place2022Renderer(Path destination) throws IOException {
        this.destination = destination;
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }

        nextState();

        int processors = Runtime.getRuntime().availableProcessors() / 2;
        LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(processors * 2);
        pool = new ThreadPoolExecutor(processors, processors, 1, TimeUnit.MINUTES, workQueue);
    }

    public static void main(String[] args) throws IOException {
        Path framesPath = Root.resolve("frames");
        Place2022Renderer renderer = new Place2022Renderer(framesPath);

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
        IndexColorModel colorModel = Utils.fromColors(Renderer.Colors2022.subList(0, paletteSize));
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
