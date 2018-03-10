package sk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.mmap.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

// Memory mapped bounded array list of long - unsafe
public final class MBArrayListLU {
    private static final Logger logger = LoggerFactory.getLogger(MBArrayListLU.class);

    private final int finalCapacity;
    private final long handle;
    private final int baseIndex;
    private final ByteBuffer buffer;

    private int size = 0;

    public MBArrayListLU
            (final IUnsafeAllocator allocator,
             final int finalCapacity) {
        this.finalCapacity = finalCapacity;
        handle = allocBuffer(allocator, finalCapacity);
        baseIndex = Utils.getBufferIndex(handle) + Constants.SIZE_OF_INT;
        buffer = getBuffer(allocator, handle);
    }

    private static long allocBuffer
            (final IUnsafeAllocator allocator,
             final int finalCapacity) {
        final int byteLen = Utils.getLongBufferLength(finalCapacity);
        final long handle = allocator.alloc(Constants.SIZE_OF_INT + byteLen);
        final ByteBuffer bb = allocator.getByteBuffer(handle);
        bb.putInt(Utils.getBufferIndex(handle) + 0, finalCapacity);
        return handle;
    }

    private static ByteBuffer getBuffer
            (final IUnsafeAllocator allocator,
             final long handle) {
        return allocator.getByteBuffer(handle);
    }

    public static long create
            (final IUnsafeAllocator allocator,
             final List<Long> list) {
        int finalCapacity = list.size();
        final long handle = allocBuffer(allocator, finalCapacity);
        final int baseIndex = Utils.getBufferIndex(handle) + Constants.SIZE_OF_INT;
        final ByteBuffer buffer = getBuffer(allocator, handle);
        for (int i = 0; i < finalCapacity; i++) {
            buffer.putLong(baseIndex + i * Constants.SIZE_OF_LONG, list.get(i));
        }
        return handle;
    }

    public long handle() {
        return handle;
    }

    public void put(final int index, final long value) {
        if (index >= finalCapacity) {
            throw new IndexOutOfBoundsException("Size is " + size + " but index is " + index);
        }

        buffer.putLong(baseIndex + index * Constants.SIZE_OF_LONG, value);
    }

    public void add(final long i) {
        if (size >= finalCapacity) {
            throw new RuntimeException("No space left in list");
        }

        buffer.putLong(baseIndex + size * Constants.SIZE_OF_LONG, i);
        ++size;
    }

    public long get(final int index) {
        return buffer.getLong(baseIndex + index * Constants.SIZE_OF_LONG);
    }

    public int size() {
        return size;
    }

    public void forEach(Consumer<Long> action) {
        for (int i = 0; i < size; i++) {
            action.accept(buffer.getLong(baseIndex + i * Constants.SIZE_OF_LONG));
        }
    }

    /**
     * It traverses the complete buffer, it is upto consumer to ensure that the
     * whole buffer was written with valid data, else unexpected thing may happen
     * @param allocator
     * @param handle
     * @param action
     */
    public static void forEach
    (final IUnsafeAllocator allocator,
     final long handle,
     final Consumer<Long> action) {
        final ByteBuffer buffer = getBuffer(allocator, handle);
        int baseIndex = Utils.getBufferIndex(handle);
        final int size = buffer.getInt(baseIndex);
        baseIndex += Constants.SIZE_OF_INT;
        for (int i = 0; i < size; i++) {
            action.accept(buffer.getLong(baseIndex + i * Constants.SIZE_OF_LONG));
        }
    }
}
