package sk.mmap;

public interface MObject {
    long handle();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
