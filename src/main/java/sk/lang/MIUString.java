package sk.lang;

import sk.mmap.Constants;
import sk.mmap.IUnsafeAllocator;
import sk.mmap.Utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public final class MIUString {

    private static CharBuffer getCharBuffer
            (final IUnsafeAllocator allocator, final long handle) {
        final ByteBuffer bb = allocator.getByteBuffer(handle);
        final int bufferOffset = Utils.getBufferIndex(handle);

        final int strlen = bb.getInt(bufferOffset + 0);
        final int strBufLen = Utils.getCharBufferLength(strlen);

        // This is must, else allocator.alloc will be messed up!
        final int currPos = bb.position();

        bb.position(bufferOffset + Constants.SIZE_OF_INT);
        final ByteBuffer cbb = bb.slice();
        cbb.limit(strBufLen);

        // Set the position back to original position
        bb.position(currPos);

        return cbb.asCharBuffer();
    }

    public static long create
            (final IUnsafeAllocator allocator,
             final CharSequence str) {
        final int strlen = str.length();
        final int strBufLen = Utils.getCharBufferLength(strlen);
        final long handle = allocator.alloc(Constants.SIZE_OF_INT + strBufLen);

        final ByteBuffer bb = allocator.getByteBuffer(handle);
        final int bufferOffset = Utils.getBufferIndex(handle);

        bb.putInt(bufferOffset + 0, strlen);
        if (strlen > 0) {
            for (int i = 0; i < strlen; i++) {
                bb.putChar(bufferOffset + Constants.SIZE_OF_INT + i * Constants.SIZE_OF_CHAR,
                        str.charAt(i));
            }
        }
        return handle;
    }

    public static CharBuffer getReadOnlyBuffer
            (final IUnsafeAllocator allocator,
             final long handle) {
        return getCharBuffer(allocator, handle).asReadOnlyBuffer();
    }
}
