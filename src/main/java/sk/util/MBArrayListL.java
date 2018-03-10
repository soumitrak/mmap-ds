package sk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.mmap.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.function.Consumer;

// Memory mapped bounded array list of long
public final class MBArrayListL {
    private static final Logger logger = LoggerFactory.getLogger(MBArrayListL.class);

    private final int finalCapacity;
    private final long handle;
    private final LongBuffer buffer;

    private int size = 0;

    public MBArrayListL
            (final IAllocator allocator,
             final int finalCapacity) {
        this.finalCapacity = finalCapacity;
        handle = allocBuffer(allocator, finalCapacity);
        buffer = getBuffer(allocator, handle);
    }

    private static long allocBuffer
            (final IAllocator allocator,
             final int finalCapacity) {
        final int byteLen = Utils.getLongBufferLength(finalCapacity);
        final long handle = allocator.alloc(Constants.SIZE_OF_INT + byteLen);
        final ByteBuffer bb = allocator.getByteBuffer(handle);
        bb.putInt(0, finalCapacity);
        return handle;
    }

    private static LongBuffer getBuffer
            (final IAllocator allocator,
             final long handle) {
        final ByteBuffer bb = allocator.getByteBuffer(handle);
        final int finalCapacity = bb.getInt(0);
        bb.position(Constants.SIZE_OF_INT);
        final ByteBuffer cbb = bb.slice();
        final int byteLen = Utils.getLongBufferLength(finalCapacity);
        cbb.limit(byteLen);
        return cbb.asLongBuffer();
    }

    public static long create
            (final IAllocator allocator,
             final List<Long> list) {
        int finalCapacity = list.size();
        final long handle = allocBuffer(allocator, finalCapacity);
        final LongBuffer buffer = getBuffer(allocator, handle);
        for (long l : list) {
            buffer.put(l);
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

        buffer.put(index, value);
    }

    public void add(final long i) {
        if (size >= finalCapacity) {
            throw new RuntimeException("No space left in list");
        }

        buffer.put(i);
        ++size;
    }

    public long get(int index) {
        return buffer.get(index);
    }

    public int size() {
        return size;
    }

    public void forEach(Consumer<Long> action) {
        for (int i = 0; i < size; i++) {
            action.accept(buffer.get(i));
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
            (final IAllocator allocator,
             final long handle,
             final Consumer<Long> action) {
        final LongBuffer buffer = getBuffer(allocator, handle);
        final int capacity = buffer.capacity();
        for (int i = 0; i < capacity; i++) {
            action.accept(buffer.get(i));
        }
    }
}
