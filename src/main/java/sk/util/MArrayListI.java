package sk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.mmap.*;

import java.nio.IntBuffer;

// Memory mapped array list of int
public class MArrayListI implements AllocObject, MObject {
    private static final Logger logger = LoggerFactory.getLogger(MArrayListI.class);

    private int _size = 0;
    private final Allocator _allocator;
    private long _allocHandle;
    private IntBuffer _buffer;

    public MArrayListI(Allocator allocator, int initialCapacity) {
        _allocator = allocator;
        _allocHandle = allocator.alloc(Utils.getIntBufferLength(initialCapacity));
        _buffer = allocator.getIntBuffer(_allocHandle);
    }

    public long handle() {
        return _allocHandle;
    }

    public void delete() {
        if (_buffer != null) {
            _buffer = null;
            _allocator.free(_allocHandle);
            _allocHandle = Constants.NULL;
        }
    }

    public void add(int i) {
        if (!_buffer.hasRemaining())
            resize();

        _buffer.put(i);
        // _buffer.put(_size, i);
        ++_size;
    }

    public int get(int index) {
        return _buffer.get(index);
    }

    public int size() {
        return _size;
    }

    private void resizeAlloc() {
        int newCapacity = Utils.getNextCapacity(_size);
        logger.debug("Buffer full, resizing capacity from {} to {}.", _size, newCapacity);
        long newHandle = _allocator.alloc(Utils.getIntBufferLength(newCapacity));
        IntBuffer newBuffer = _allocator.getIntBuffer(newHandle);
        _buffer.rewind();
        newBuffer.put(_buffer);
        _allocator.free(_allocHandle);
        _allocHandle = newHandle;
        _buffer = newBuffer;
    }

    private MArrayListI resize() {
        // TODO: Account for overflow
        int newCapacity = Utils.getNextCapacity(_size);
        logger.debug("Buffer full, resizing capacity from {} to {}.", _size, newCapacity);
        long newHandle = _allocator.tryrealloc(_allocHandle, Utils.getIntBufferLength(newCapacity));

        if (newHandle == _allocHandle) {
            logger.debug("Extended current buffer.");
            _buffer = _allocator.getIntBuffer(_allocHandle);
            _buffer.position(_size);
        } else {
            IntBuffer newBuffer = _allocator.getIntBuffer(newHandle);
            _buffer.rewind();
            newBuffer.put(_buffer);
            _allocator.free(_allocHandle);
            _allocHandle = newHandle;
            _buffer = newBuffer;
        }

        return this;
    }
}
