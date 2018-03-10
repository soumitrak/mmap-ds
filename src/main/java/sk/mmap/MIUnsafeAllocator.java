package sk.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

// Memory mapped, immutable, unsafe (you should know what you are doing), allocator
// unsafe - it give access to the underlying arena, without limiting size/offset
//        - All read/write must be done using offset, else things will go WRONG!
//        - Don't change its position

public class MIUnsafeAllocator implements IUnsafeAllocator {
    private static final Logger logger = LoggerFactory.getLogger(MIUnsafeAllocator.class);

    private final ByteBufferProvider byteBufferProvider;

    protected final ByteBuffer[] arenas =
            new MappedByteBuffer[Constants.MMAP_ARENA_COUNT];

    public MIUnsafeAllocator(final ByteBufferProvider byteBufferProvider) {
        this.byteBufferProvider = byteBufferProvider;
    }

    public void debug() {
        byteBufferProvider.debug();
    }

    protected final int getNumArenas() {
        return arenas.length;
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

    public void close() {
        byteBufferProvider.close();
    }

    protected ByteBuffer getOrAllocArena(int index) throws OutOfMemoryError {
        if (index >= arenas.length) {
            throw new IndexOutOfBoundsException("Arena index is " + index + " max is " + arenas.length);
        }

        ByteBuffer arena = arenas[index];
        if (arena == null) {
            try {
                arena = byteBufferProvider.getNextBuffer();
                arenas[index] = arena;
            } catch (IOException e) {
                // logger.error("mmap failed: {}", e.printStackTrace());
                throw new OutOfMemoryError(e.getMessage());
            }
        }

        return arena;
    }

    protected ByteBuffer getArena
            (final long handle)
            throws OutOfMemoryError {
        if (handle < 0)
            throw new OutOfMemoryError("Illegal handle " + handle);

        int arenaIndex = Utils.getArenaIndex(handle);
        if (arenaIndex < 0 || arenaIndex >= Constants.MMAP_ARENA_COUNT) {
            throw new OutOfMemoryError("Arena index is out of bounds " + arenaIndex + ", arena count is " + Constants.MMAP_ARENA_COUNT);
        }

        final ByteBuffer arena = arenas[arenaIndex];
        if (arena == null) {
            throw new OutOfMemoryError("Arena " + arenaIndex + " is not allocated yet.");
        }

        return arena;
    }

    public ByteBuffer getByteBuffer(final long handle) throws OutOfMemoryError {
        return getArena(handle);
    }
}
