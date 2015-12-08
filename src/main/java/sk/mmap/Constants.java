package sk.mmap;

public final class Constants {
    public static final int SIZE_OF_BYTE = 1;
    public static final int SIZE_OF_SHORT = 2;
    public static final int SIZE_OF_INT = 4;
    public static final int SIZE_OF_LONG = 8;
    public static final int SIZE_OF_FLOAT = 32;
    public static final int SIZE_OF_DOUBLE = 64;
    public static final int SIZE_OF_CHAR = 2;

    public static final int NULL = -1;

    // Direct ByteBuffer size.
    public static final int DIRECT_BUFFER_SIZE = Integer.MAX_VALUE; // 1024 * 1024 * 1024;

    // mmap buffer size
    public static final long MMAP_BUFFER_SIZE = Integer.MAX_VALUE; // 1024 * 1024 * 1024;

    // Maximum number of mmap'ed buffers per file.
    public static final int MMAP_BUFFER_COUNT = 128;

    // Byte length of header in allocated buffer by Allocator.
    // 24 bits unused
    // +
    // 8 bits of index into mmap'ed array of buffers
    // +
    // 32 bits of index into raw mmap'ed buffer
    // +
    // 32 bits for capacity of buffer allocated, total size (header + asked + alignment).
    // +
    // 32 bits for asked size.
    public static final int ALLOC_BUFFER_HANDLE_LENGTH = SIZE_OF_LONG;

    // TODO: Do I need space for tail? Will be useful in reducing fragmentation.
    public static final int ALLOC_BUFFER_HEADER_LENGTH =
            ALLOC_BUFFER_HANDLE_LENGTH +
                    SIZE_OF_INT +
                    SIZE_OF_INT;

    public static final int ALLOC_BUFFER_ALIGN_SIZE = ALLOC_BUFFER_HANDLE_LENGTH;
    public static final int ALLOC_BUFFER_ASK_SIZE = ALLOC_BUFFER_HANDLE_LENGTH + SIZE_OF_INT;

    public static final int FREE_LIST_PREV = ALLOC_BUFFER_HEADER_LENGTH;
    public static final int FREE_LIST_NEXT = ALLOC_BUFFER_HEADER_LENGTH + ALLOC_BUFFER_HANDLE_LENGTH;

    // Align buffer allocation to 64 bit boundary.
    private static final int ALLOC_SIZE_ALIGNMENT = 8;
    public static final int ALLOC_SIZE_ALIGNMENT_ADD = ALLOC_SIZE_ALIGNMENT - 1;
    public static final int ALLOC_SIZE_ALIGNMENT_MASK = ~ALLOC_SIZE_ALIGNMENT_ADD;
}
