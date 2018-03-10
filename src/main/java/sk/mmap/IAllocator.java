package sk.mmap;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Immutable allocator
 * Does not support free/realloc, so does not need to allocate larger buffer
 * than asked to support free list.
 * No overhead of boundary alignment
 * Do not store allocated size event - lets see if this possible to make it work
 */
public interface IAllocator extends IUnsafeAllocator {

    // Internal API
    // ByteBuffer getArena(final long handle) throws OutOfMemoryError;

    // ShortBuffer
    ShortBuffer getShortBuffer(final long handle) throws OutOfMemoryError;

    // IntBuffer
    IntBuffer getIntBuffer(final long handle) throws OutOfMemoryError;

    // LongBuffer
    LongBuffer getLongBuffer(final long handle) throws OutOfMemoryError;

    // FloatBuffer
    FloatBuffer getFloatBuffer(final long handle) throws OutOfMemoryError;

    // DoubleBuffer
    DoubleBuffer getDoubleBuffer(final long handle) throws OutOfMemoryError;

    // CharBuffer
    CharBuffer getCharBuffer(final long handle) throws OutOfMemoryError;
}
