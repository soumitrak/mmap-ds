package sk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.mmap.*;

import java.nio.LongBuffer;
import java.util.List;

// Memory mapped array list of long
public class MArrayListL implements IAllocObject, MObject {
    private static final Logger logger = LoggerFactory.getLogger(MArrayListL.class);

    private int _size = 0;
    private final Allocator _allocator;
    private long _allocHandle;
    private LongBuffer _buffer;

    public MArrayListL(Allocator allocator, int initialCapacity) {
        _allocator = allocator;
        _allocHandle = allocator.alloc(Utils.getLongBufferLength(initialCapacity));
        _buffer = allocator.getLongBuffer(_allocHandle);
    }

    public static long create
            (final Allocator allocator,
             final List<Long> list) {
        final long handle = allocator.alloc(Utils.getLongBufferLength
                (list.isEmpty() ? 1 : list.size() + 1));
        final LongBuffer buffer = allocator.getLongBuffer(handle);
        // TODO: Add size in MArrayListL also
        buffer.put(list.size());
        for (long l : list) {
            buffer.put(l);
        }
        return handle;
    }

    public long handle() {
        return _allocHandle;
    }

    /*public void delete() {
        if (_buffer != null) {
            _buffer = null;
            _allocator.free(_allocHandle);
            _allocHandle = Constants.NULL;
        }
    }*/

    public void put(final long i) {
        if (!_buffer.hasRemaining())
            resize();

        _buffer.put(i);
        // _buffer.put(_size, i);
        ++_size;
    }

    public void put(final int index, final long value) {
        if (index >= _size) {
            throw new IndexOutOfBoundsException("Size is " + _size + " but index is " + index);
        }

        _buffer.put(index, value);
    }

    public long get(int index) {
        return _buffer.get(index);
    }

    public int size() {
        return _size;
    }

    private void resizeAlloc() {
        int newCapacity = Utils.getNextCapacity(_size);
        logger.debug("Buffer full, resizing capacity from {} to {}.", _size, newCapacity);
        long newHandle = _allocator.alloc(Utils.getLongBufferLength(newCapacity));
        LongBuffer newBuffer = _allocator.getLongBuffer(newHandle);
        _buffer.rewind();
        newBuffer.put(_buffer);
        _allocator.free(_allocHandle);
        _allocHandle = newHandle;
        _buffer = newBuffer;
    }

    private MArrayListL resize() {
        // TODO: Account for overflow
        int newCapacity = Utils.getNextCapacity(_size);
        logger.debug("Buffer full, resizing capacity from {} to {}.", _size, newCapacity);
        long newHandle = _allocator.tryrealloc(_allocHandle, Utils.getLongBufferLength(newCapacity));

        if (newHandle == _allocHandle) {
            logger.debug("Extended current buffer.");
            _buffer = _allocator.getLongBuffer(_allocHandle);
            _buffer.position(_size);
        } else {
            LongBuffer newBuffer = _allocator.getLongBuffer(newHandle);
            _buffer.rewind();
            newBuffer.put(_buffer);
            _allocator.free(_allocHandle);
            _allocHandle = newHandle;
            _buffer = newBuffer;
        }

        return this;
    }
}
