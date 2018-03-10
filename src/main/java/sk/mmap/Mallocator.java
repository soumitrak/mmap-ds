package sk.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.*;

public class Mallocator extends MIAllocator implements Allocator {
    private static final Logger logger = LoggerFactory.getLogger(Mallocator.class);

    private long _freeListHead = Constants.NULL;

    // private final ByteBufferProvider byteBufferProvider;

//    private final ByteBuffer[] arenas =
//            new MappedByteBuffer[Constants.MMAP_ARENA_COUNT];

    public Mallocator(final ByteBufferProvider byteBufferProvider) {
        super(byteBufferProvider);
    }

    private void writeHeaderSizes(ByteBuffer buffer, int offset, int size, int alignedSize) {
        if (alignedSize <= 0)
            throw new RuntimeException("alignedSize is " + alignedSize);

        // Write allocated raw buffer size.
        buffer.putInt(offset + Constants.ALLOC_BUFFER_ALIGN_SIZE, alignedSize);

        // Write asked buffer size, consumer of this buffer is not allowed to
        // read/write past asked bytes. Even though there may be some space left.
        buffer.putInt(offset + Constants.ALLOC_BUFFER_ASK_SIZE, size);
    }

    // TODO: Implement first fit or best fit

    private long allocFromFreeList(int size, int alignedSize) {
        long ptr = _freeListHead;

        while (ptr != Constants.NULL) {
            ByteBuffer rawBuffer = getRawBuffer(ptr);

            // Check if current buffer has enough space, pick the best fit.
            // If the free list has large buffers, don't use it.
            int oldAlignedSize = rawBuffer.getInt(Constants.ALLOC_BUFFER_ALIGN_SIZE);
            // If the buffer size is > alignedSize, and less than 2*alignedSize, allocate it.
            if (alignedSize == oldAlignedSize ||
                    oldAlignedSize / alignedSize == 1) {
                // Remove this node from list.
                long prev = rawBuffer.getLong(Constants.FREE_LIST_PREV);
                long next = rawBuffer.getLong(Constants.FREE_LIST_NEXT);

                if (prev != Constants.NULL) {
                    ByteBuffer prevBuffer = getRawBuffer(prev);
                    prevBuffer.putLong(Constants.FREE_LIST_NEXT, next);
                }

                if (next != Constants.NULL) {
                    ByteBuffer nextBuffer = getRawBuffer(next);
                    nextBuffer.putLong(Constants.FREE_LIST_PREV, prev);
                }

                // Set _freeListHead if needed.
                if (ptr == _freeListHead)
                    _freeListHead = next;

                // Allocate this buffer, but preserve the original size.
                writeHeaderSizes(rawBuffer, 0, size, oldAlignedSize);
                logger.debug("Allocated from free list size {} aligned size {}.", size, alignedSize);
                break;
            }

            ptr = rawBuffer.getLong(Constants.FREE_LIST_NEXT);
        }

        return ptr;
    }

    public long alloc(final int size) throws OutOfMemoryError {
        if (size <= 0)
            throw new OutOfMemoryError("Trying to alloc " + size + " bytes.");

        int alignedSize = Utils.getAllocSize(size);

        // logger.debug("Trying to alloc size {} aligned size {}.", size, alignedSize);

        long freeHandle = allocFromFreeList(size, alignedSize);
        if (freeHandle != Constants.NULL)
            return freeHandle;

        // Not found in free list.
        for (int i = 0; i < arenas.length; i++) {
            ByteBuffer buffer = getOrAllocArena(i);

            // Don't fill the buffer to exactly 2G size, otherwise Buffer.limit to 2G + 1 becomes negative and it fails.
            /*
                Exception in thread "main" java.lang.IllegalArgumentException
	            at java.nio.Buffer.limit(Buffer.java:275)
	            at sk.mmap.Mallocator.getRawBuffer(Mallocator.java:245)
	            at sk.mmap.Mallocator.getByteBuffer(Mallocator.java:256)
             */
            if (alignedSize <= buffer.remaining()) {
                // TODO: Put some magic bytes to check if it is a valid allocated buffer
                //       to check for invalid free/getBuffer.

                int currentPosition = buffer.position();
                // Move the raw buffer's position to next available location.
                buffer.position(currentPosition + alignedSize);

                long handle = Utils.getHandle(i, currentPosition);

                // Write header
                // Write handle.
                buffer.putLong(currentPosition, handle);
                writeHeaderSizes(buffer, currentPosition, size, alignedSize);

                return handle;
            }

        }

        throw new OutOfMemoryError("Failed to allocate " + size + " bytes.");
    }

    public long realloc(long handle, int size) throws OutOfMemoryError {
        if (size <= 0)
            throw new OutOfMemoryError("Trying to alloc " + size + " bytes.");

        if (handle < 0)
            throw new OutOfMemoryError("Illegal handle " + handle);

        int alignedSize = Utils.getAllocSize(size);

        logger.debug("Trying to realloc size {} aligned size {}.", size, alignedSize);

        ByteBuffer rawBuffer = getRawBuffer(handle);

        // Get the total allocated size.
        int oldAlignedSize = rawBuffer.getInt(Constants.ALLOC_BUFFER_ALIGN_SIZE);

        if (alignedSize <= oldAlignedSize) {
            // Previous buffer is big enough to hold new size.
            // Just modify the size and return.
            rawBuffer.putInt(Constants.ALLOC_BUFFER_ASK_SIZE, size);
            return handle;
        } else {
            int arrayIndex = Utils.getArenaIndex(handle);
            int index = Utils.getBufferIndex(handle);

            ByteBuffer buffer = arenas[arrayIndex];
            int currentPosition = buffer.position();
            if (currentPosition == index + oldAlignedSize) {
                // Buffer is available to realloc.

                // Check if there are enough bytes left.
                int remaining = buffer.remaining();
                if (alignedSize <= oldAlignedSize + remaining) {
                    // There are enough space left to realloc.
                    buffer.position(index + alignedSize);

                    writeHeaderSizes(rawBuffer, 0, size, alignedSize);

                    return handle;
                } else {
                    // Not enough space to left to realloc.
                    logger.debug("Failed to extend allocated buffer, asked bytes {} aligned bytes {} available bytes {}.",
                            size, alignedSize, oldAlignedSize + remaining);
                    throw new OutOfMemoryError("Failed to realloc");
                }
            } else {
                // Buffer cannot be extended.
                logger.debug("Failed to extend allocated buffer, it is already allocated after current allocated buffer.");
                throw new OutOfMemoryError("Failed to realloc");
            }
        }
    }

    public long tryrealloc(long handle, int size) throws OutOfMemoryError {
        try {
            return realloc(handle, size);
        } catch (OutOfMemoryError e) {
            return alloc(size);
        }
    }

    public Allocator free(long handle) {
        if (handle >= 0) {
            // Free list is a doubly linked list. Add the free buffer to beginning of the list.

            // Set the previous link of the head node.
            if (_freeListHead != Constants.NULL)
                getRawBuffer(_freeListHead).
                        putLong(Constants.FREE_LIST_PREV, handle);

            // Set the asked buffer size to zero, and leave the aligned size as it is.
            // Set the prev, and next link of current node.
            getRawBuffer(handle).
                    putInt(Constants.ALLOC_BUFFER_ASK_SIZE, 0).
                    putLong(Constants.FREE_LIST_PREV, Constants.NULL).
                    putLong(Constants.FREE_LIST_NEXT, _freeListHead);

            _freeListHead = handle;
        }

        return this;
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

        // Set position and limit of the allocated arena.
        // TODO: Is there a way to set capacity?
        int limit = arena.getInt(index + Constants.SIZE_OF_LONG);
        try {
            allocatedBuffer.limit(limit);
        } catch (java.lang.IllegalArgumentException e) {
            logger.error("Error during setting allocatedBuffer limit to " + limit + " index " + index + " arenaIndex " + Utils.getArenaIndex(handle));
            throw e;
        }
        // allocatedBuffer.position(Constants.ALLOC_BUFFER_HEADER_LENGTH);

        return allocatedBuffer;
    }

    private int getDataOffset() {
        return Constants.ALLOC_BUFFER_HEADER_LENGTH;
    }

    public ByteBuffer getByteBuffer(long handle) throws OutOfMemoryError {
        ByteBuffer rawBuffer = getRawBuffer(handle);
        int askedSize = rawBuffer.getInt(Constants.SIZE_OF_LONG + Constants.SIZE_OF_INT);
        rawBuffer.position(Constants.ALLOC_BUFFER_HEADER_LENGTH);
        ByteBuffer tmp = rawBuffer.slice();
        tmp.limit(askedSize);
        return tmp;
    }

    /*public ShortBuffer getShortBuffer(long handle) throws OutOfMemoryError {
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
    }*/

    public void debug() {
        super.debug();

        // Print each buffer.
        for (int i = 0; i < arenas.length; i++) {
            ByteBuffer buffer = arenas[i];
            if (buffer != null) {
                // buffer.reset();
                if (buffer.hasRemaining())
                    logger.debug("Buffer {} has {} remaining bytes, its capacity is {}.", i, buffer.remaining(), buffer.capacity());
                else
                    logger.debug("Buffer {} is full, its capacity is {}.", i, buffer.capacity());
            }
        }

        if (_freeListHead != Constants.NULL) {
            logger.debug("Free list is:");
            long ptr = _freeListHead;
            while (ptr != Constants.NULL) {
                int arrayIndex = Utils.getArenaIndex(ptr);
                int index = Utils.getBufferIndex(ptr);
                ByteBuffer rawBuffer = getRawBuffer(ptr);
                int size = rawBuffer.getInt(Constants.ALLOC_BUFFER_ALIGN_SIZE);
                int ask = rawBuffer.getInt(Constants.ALLOC_BUFFER_ASK_SIZE);
                logger.debug("-> arrayIndex {}, index {}, size {}.", arrayIndex, index, size);
                ptr = rawBuffer.getLong(Constants.FREE_LIST_NEXT);
            }
        } else {
            logger.debug("Free list is empty.");
        }
    }
}
