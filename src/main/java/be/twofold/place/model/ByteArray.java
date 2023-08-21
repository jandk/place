package be.twofold.place.model;

import java.util.*;

public record ByteArray(byte[] array) implements Comparable<ByteArray> {
    public ByteArray {
        Objects.requireNonNull(array, "array is null");
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
