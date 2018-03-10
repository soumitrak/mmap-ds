package sk.mmap;

import java.nio.ByteBuffer;

public interface IUnsafeAllocator {
    long alloc(final int size) throws OutOfMemoryError;

    void close();

    void debug();

    // Returns underlying ByteBuffer
    ByteBuffer getByteBuffer(final long handle) throws OutOfMemoryError;
}
