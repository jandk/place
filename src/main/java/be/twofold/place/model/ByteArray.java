package be.twofold.place.model;

import java.util.Arrays;
import java.util.Objects;

public final class ByteArray implements Comparable<ByteArray> {
    private final byte[] array;

    public ByteArray(byte[] array) {
        this.array = Objects.requireNonNull(array);
    }

    public byte[] getArray() {
        return array;
    }

    @Override
    public int compareTo(ByteArray other) {
        return Arrays.compareUnsigned(array, other.array);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof ByteArray
            && Arrays.equals(array, ((ByteArray) obj).array);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }

    @Override
    public String toString() {
        return Arrays.toString(array);
    }
}
