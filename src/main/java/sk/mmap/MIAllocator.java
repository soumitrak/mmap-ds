package sk.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;

// Memory mapped, immutable allocator
public class MIAllocator extends MIUnsafeAllocator implements IAllocator {
    private static final Logger logger = LoggerFactory.getLogger(MIAllocator.class);

    public MIAllocator(final ByteBufferProvider byteBufferProvider) {
        super(byteBufferProvider);
    }

    public long alloc(final int size) throws OutOfMemoryError {
        if (size <= 0)
            throw new OutOfMemoryError("Trying to alloc " + size + " bytes.");

        for (int i = 0; i < arenas.length; i++) {
            final ByteBuffer buffer = getOrAllocArena(i);
            if (size <= buffer.remaining()) {
                int currentPosition = buffer.position();
                // Move the raw buffer's position to next available location.
                buffer.position(currentPosition + size);
                return Utils.getHandle(i, currentPosition);
            }
        }

        throw new OutOfMemoryError("Failed to allocate " + size + " bytes.");
    }

    private ByteBuffer getRawBuffer(long handle) throws OutOfMemoryError {
        final ByteBuffer arena = getArena(handle);
        int index = Utils.getBufferIndex(handle);

        if (index < 0 || index >= arena.capacity()) {
            throw new OutOfMemoryError("Buffer index is out of bounds " + index + ", capacity is " + arena.capacity());
        }

        // Set the arena to its free location after slice.
        int current = arena.position();
        arena.position(index);
        ByteBuffer allocatedBuffer = arena.slice();
        arena.position(current);

        return allocatedBuffer;
    }

    public ByteBuffer getByteBuffer(final long handle) throws OutOfMemoryError {
        return getRawBuffer(handle);
    }

    public ShortBuffer getShortBuffer(long handle) throws OutOfMemoryError {
        return getByteBuffer(handle).asShortBuffer();
    }

    public IntBuffer getIntBuffer(long handle) throws OutOfMemoryError {
        return getByteBuffer(handle).asIntBuffer();
    }

    public LongBuffer getLongBuffer(long handle) throws OutOfMemoryError {
        return getByteBuffer(handle).asLongBuffer();
    }

    public FloatBuffer getFloatBuffer(long handle) throws OutOfMemoryError {
        return getByteBuffer(handle).asFloatBuffer();
    }

    public DoubleBuffer getDoubleBuffer(long handle) throws OutOfMemoryError {
        return getByteBuffer(handle).asDoubleBuffer();
    }

    public CharBuffer getCharBuffer(long handle) throws OutOfMemoryError {
        return getByteBuffer(handle).asCharBuffer();
    }
}
