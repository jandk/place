package be.twofold.place.model;

/**
 * The placement of a single pixel by a user
 */
public final class Placement {
    private final long timestamp;
    private final int user;
    private final int x;
    private final int y;
    private final int color;

    public Placement(long timestamp, int user, int x, int y, int color) {
        this.timestamp = timestamp;
        this.user = user;
        this.x = x;
        this.y = y;
        this.color = color;
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
        result = 31 * result + Integer.hashCode(x);
        result = 31 * result + Integer.hashCode(y);
        result = 31 * result + Integer.hashCode(color);
        return result;
    }

    @Override
    public String toString() {
        return timestamp + "," + user + "," + x + "," + y + "," + color;
    }
}
