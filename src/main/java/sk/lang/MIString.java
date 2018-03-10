package sk.lang;

import sk.mmap.Constants;
import sk.mmap.IAllocator;
import sk.mmap.Utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public final class MIString {

    private static CharBuffer getCharBuffer(final IAllocator allocator, final long handle) {
        final ByteBuffer bb = allocator.getByteBuffer(handle);
        final int strlen = bb.getInt(0);
        final int strBufLen = Utils.getCharBufferLength(strlen);
        bb.position(Constants.SIZE_OF_INT);
        final ByteBuffer cbb = bb.slice();
        cbb.limit(strBufLen);
        return cbb.asCharBuffer();
    }

    public static long create(final IAllocator allocator, final CharSequence str) {
        final int strlen = str.length();
        final int strBufLen = Utils.getCharBufferLength(strlen);
        final long handle = allocator.alloc(Constants.SIZE_OF_INT + strBufLen);

        final ByteBuffer bb = allocator.getByteBuffer(handle);
        bb.putInt(0, strlen);
        if (strlen > 0) {
            bb.position(Constants.SIZE_OF_INT);
            final ByteBuffer cbb = bb.slice();
            cbb.limit(strBufLen);
            final CharBuffer buffer = cbb.asCharBuffer();

            for (int i = 0; i < strlen; i++) {
                buffer.put(str.charAt(i));
            }
        }
        return handle;
    }

    public static CharBuffer getReadOnlyBuffer
            (final IAllocator allocator,
             final long handle) {
        return getCharBuffer(allocator, handle).asReadOnlyBuffer();
    }
}
