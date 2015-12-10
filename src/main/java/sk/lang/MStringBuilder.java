package sk.lang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.mmap.*;

import java.nio.CharBuffer;

public class MStringBuilder implements AllocObject, MObject, CharSequence {
    private static final Logger logger = LoggerFactory.getLogger(MStringBuilder.class);

    private int _length = 0;
    private final Allocator _allocator;
    private long _allocHandle;
    private CharBuffer _buffer;

    public MStringBuilder(Allocator allocator, int initialCapacity) {
        _allocator = allocator;
        _allocHandle = allocator.alloc(Utils.getCharBufferLength(initialCapacity));
        _buffer = allocator.getCharBuffer(_allocHandle);
    }

    public int length() {
        return _length;
    }

    public char charAt(int index) {
        return _buffer.get(index);
    }

    public MStringBuilder clear() {
        _length = 0;
        _buffer.rewind();
        return this;
    }

    public void delete() {
        if (_buffer != null) {
            _buffer = null;
            _allocator.free(_allocHandle);
            _allocHandle = Constants.NULL;
        }
    }

    private MStringBuilder ensureCapacity(int minimumCapacity) {
        int currentCapacity = _length + _buffer.remaining();
        if (minimumCapacity > currentCapacity) {
            resize(minimumCapacity);
        }
        return this;
    }

    private MStringBuilder addCapacity(int more) {
        int remaining = _buffer.remaining();
        if (more > remaining) {
            ensureCapacity(_length + remaining + more);
        }

        return this;
    }

    // Called when buffer is full.
    private MStringBuilder resize() {
        return resize(Utils.getNextCapacity(_length));
    }

    private MStringBuilder resize(int capacity) {
        logger.debug("Resize buffer to {} capacity.", capacity);
        long newHandle = _allocator.tryrealloc(_allocHandle, Utils.getCharBufferLength(capacity));

        if (newHandle == _allocHandle) {
            logger.debug("Extended current buffer.");
            _buffer = _allocator.getCharBuffer(_allocHandle);
            _buffer.position(_length);
        } else {
            CharBuffer newBuffer = _allocator.getCharBuffer(newHandle);
            _buffer.rewind();
            newBuffer.put(_buffer);
            _allocator.free(_allocHandle);
            _allocHandle = newHandle;
            _buffer = newBuffer;
        }

        return this;
    }

    public MStringBuilder append(boolean b) {
        if (b)
            return append("true");
        else
            return append("false");
    }

    public MStringBuilder append(char c) {
        if (!_buffer.hasRemaining())
            resize();

        _buffer.put(c);
        ++_length;

        return this;
    }

    public MStringBuilder append(int i) {
        return append(Integer.toString(i));
    }

    public MStringBuilder append(float f) {
        return append(Float.toString(f));
    }

    public MStringBuilder append(double d) {
        return append(Double.toString(d));
    }

    public MStringBuilder append(CharSequence cs) {
        for (int i = 0; i < cs.length(); i++)
            append(cs.charAt(i));

        return this;
    }

    public CharSequence subSequence(int start, int end) {
        return this.new SubSequence(start, end);
    }

    public class SubSequence implements CharSequence, AllocObject {

        private final int _start;
        private final int _end;

        SubSequence(int start, int end) {
            _start = start;
            _end = end;
        }

        public int length() {
            return _end - _start;
        }

        public char charAt(int index) {
            return MStringBuilder.this.charAt(_start + index);
        }

        public CharSequence subSequence(int start, int end) {
            return MStringBuilder.this.new SubSequence(_start + start, _end - end);
        }

        public void delete() {
            // No OP.
            // Buffer is owned by parent class.
        }
    }

    public MString toMString() {
        long handle = _allocator.alloc(Utils.getCharBufferLength(length()));
        CharBuffer buffer = _allocator.getCharBuffer(handle);
        buffer.put((CharBuffer) _buffer.flip());
        return new MString(_allocator, handle);
    }
}
