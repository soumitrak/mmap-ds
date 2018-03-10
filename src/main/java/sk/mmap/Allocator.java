package sk.mmap;

import java.nio.*;

public interface Allocator extends IAllocator {
    // Allocate a ByteBuffer of size 'size', returns Constants.NULL if fails
    // non-negative if succeeds.
    // Internally, it will result in
    // <size of magic byte sequence> +
    // <pointer to MappedByteBuffer> +
    // <offset in raw MappedByteBuffer> +
    // size + <additional bytes because of bit boundary alignment>
    // long alloc(int size) throws OutOfMemoryError;

    // Extend the previously allocated buffer to new size.
    long realloc(long handle, int size) throws OutOfMemoryError;

    // Try to realloc, if it fails then alloc.
    long tryrealloc(long handle, int size) throws OutOfMemoryError;

    // Free the buffer.
    Allocator free(long handle);

    // void close();

    // Allocator debug();

    // Get the underlying readable/writable buffers only excluding header, and align bytes.

    // ByteBuffer
    // ByteBuffer getByteBuffer(long handle) throws OutOfMemoryError;

    // ShortBuffer
    // ShortBuffer getShortBuffer(long handle) throws OutOfMemoryError;

    // IntBuffer
    // IntBuffer getIntBuffer(long handle) throws OutOfMemoryError;

    // LongBuffer
    // LongBuffer getLongBuffer(long handle) throws OutOfMemoryError;

    // FloatBuffer
    // FloatBuffer getFloatBuffer(long handle) throws OutOfMemoryError;

    // DoubleBuffer
    // DoubleBuffer getDoubleBuffer(long handle) throws OutOfMemoryError;

    // CharBuffer
    // CharBuffer getCharBuffer(long handle) throws OutOfMemoryError;

    // TODO: Reserve space for magic byte sequence.
    // Reading/writing MUST happen after this offset.
    // int getDataOffset();
}
