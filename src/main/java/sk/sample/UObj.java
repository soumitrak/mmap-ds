package sk.sample;

import sk.mmap.Constants;
import sk.mmap.IUnsafeAllocator;
import sk.mmap.Utils;

public final class UObj {
    // long id
    private static int ID_SIZE = Constants.SIZE_OF_LONG;
    private static int ID_OFFSET = 0;

    // char type
    private static int TYPE_SIZE = Constants.SIZE_OF_CHAR;
    public static int TYPE_OFFSET = ID_OFFSET + ID_SIZE;

    // MString name
    private static int NAME_SIZE = Constants.SIZE_OF_LONG;
    public static int NAME_OFFSET = TYPE_OFFSET + TYPE_SIZE;

    private static int sizeof() {
        return ID_SIZE + TYPE_SIZE + NAME_SIZE;
    }

    public static long create
            (final IUnsafeAllocator allocator,
             final long id,
             final char type,
             final long name) {
        final long handle = allocator.alloc(sizeof());
        final int bufferOffset = Utils.getBufferIndex(handle);
        allocator.getByteBuffer(handle)
                .putLong(bufferOffset + ID_OFFSET, id)
                .putChar(bufferOffset + TYPE_OFFSET, type)
                .putLong(bufferOffset + NAME_OFFSET, name);
        return handle;
    }

    public static long getId
            (final IUnsafeAllocator allocator,
             final long handle) {
        return allocator.getByteBuffer(handle)
                .getLong(Utils.getBufferIndex(handle) + ID_OFFSET);
    }
}
