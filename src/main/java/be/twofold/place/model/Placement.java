package be.twofold.place.model;

/**
 * The placement of a single pixel by a user
 */
public final class Placement implements Comparable<Placement> {
    private final long timestamp;
    private final int user;
    private final short x;
    private final short y;
    private final int color;

    public Placement(long timestamp, int user, short x, short y, int color) {
        this.timestamp = timestamp;
        this.user = user;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public static Placement parse(String s) {
        int i1 = s.indexOf(',');
        int i2 = s.indexOf(',', i1 + 1);
        int i3 = s.indexOf(',', i2 + 1);
        int i4 = s.indexOf(',', i3 + 1);

        return new Placement(
            Long.parseLong(s, 0, i1, 10),
            Integer.parseInt(s, i1 + 1, i2, 10),
            (short) Integer.parseInt(s, i2 + 1, i3, 10),
            (short) Integer.parseInt(s, i3 + 1, i4, 10),
            Integer.parseInt(s, i4 + 1, s.length(), 10)
        );
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getUser() {
        return user;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getColor() {
        return color;
    }

    @Override
    public int compareTo(Placement o) {
        return Long.compare(timestamp, o.timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Placement)) return false;

        Placement placement = (Placement) obj;
        return timestamp == placement.timestamp
            && user == placement.user
            && x == placement.x
            && y == placement.y
            && color == placement.color;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Long.hashCode(timestamp);
        result = 31 * result + Integer.hashCode(user);
        result = 31 * result + Short.hashCode(x);
        result = 31 * result + Short.hashCode(y);
        result = 31 * result + Integer.hashCode(color);
        return result;
    }

    @Override
    public String toString() {
        return timestamp + "," + user + "," + x + "," + y + "," + color;
    }
}
