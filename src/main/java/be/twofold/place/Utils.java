package be.twofold.place;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static <T> void writeAll(Path outputPath, Collection<T> collection) {
        writeAll(outputPath, collection, Objects::toString);
    }

    public static <T> void writeAll(Path outputPath, Collection<T> collection, Function<? super T, String> mapper) {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            collection.stream()
                .map(mapper)
                .forEach(line -> {
                    try {
                        writer.write(line);
                        writer.write('\n');
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static IndexColorModel fromColors(List<Color> colors) {
        byte[] r = new byte[colors.size()];
        byte[] g = new byte[colors.size()];
        byte[] b = new byte[colors.size()];

        for (int i = 0; i < colors.size(); i++) {
            r[i] = (byte) colors.get(i).getRed();
            g[i] = (byte) colors.get(i).getGreen();
            b[i] = (byte) colors.get(i).getBlue();
        }
        return new IndexColorModel(colors.size() > 16 ? 5 : 4, colors.size(), r, g, b);
    }

}
