package be.twofold.place;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.util.List;

public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
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

    static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
