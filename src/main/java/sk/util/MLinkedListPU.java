package sk.util;

import sk.mmap.Constants;
import sk.mmap.IUnsafeAllocator;
import sk.mmap.Utils;

import java.nio.ByteBuffer;

// Memory mapped linked list of pointers - Unsafe
public class MLinkedListPU {
    private static class Node {
        // Pointer to any object
        private static int P_SIZE = Constants.SIZE_OF_LONG;
        private static int P_OFFSET = 0;

        // Pointer to next Node
        private static int NP_SIZE = Constants.SIZE_OF_LONG;
        private static int NP_OFFSET = P_OFFSET + P_SIZE;

        private static int sizeof() {
            return P_SIZE + NP_SIZE;
        }

        private static long create
                (final IUnsafeAllocator allocator,
                 final long p,
                 final long np) {
            final long handle = allocator.alloc(sizeof());
            final int bufferOffset = Utils.getBufferIndex(handle);
            allocator.getByteBuffer(handle)
                    .putLong(bufferOffset + P_OFFSET, p)
                    .putLong(bufferOffset + NP_OFFSET, np);
            return handle;
        }

        private static long setNext
                (final IUnsafeAllocator allocator,
                 final long handle,
                 final long np) {
            allocator.getByteBuffer(handle)
                    .putLong(Utils.getBufferIndex(handle) + NP_OFFSET, np);
            return handle;
        }

        private static long getP
                (final IUnsafeAllocator allocator,
                 final long handle) {
            return allocator.getByteBuffer(handle)
                    .getLong(Utils.getBufferIndex(handle) + P_OFFSET);
        }

        private static long getNext
                (final IUnsafeAllocator allocator,
                 final long handle) {
            return allocator.getByteBuffer(handle)
                    .getLong(Utils.getBufferIndex(handle) + NP_OFFSET);
        }
    }

    // long first;
    private static int FIRST_SIZE = Constants.SIZE_OF_LONG;
    private static int FIRST_OFFSET = 0;

    // long last;
    private static int LAST_SIZE = Constants.SIZE_OF_LONG;
    private static int LAST_OFFSET = FIRST_OFFSET + FIRST_SIZE;

    private static int sizeof() {
        return FIRST_SIZE + LAST_SIZE;
    }

    public static long create
            (final IUnsafeAllocator allocator) {
        final long list = allocator.alloc(sizeof());
        final int bufferOffset = Utils.getBufferIndex(list);
        allocator.getByteBuffer(list)
                .putLong(bufferOffset + FIRST_OFFSET, Constants.NULL)
                .putLong(bufferOffset + LAST_OFFSET, Constants.NULL);
        return list;
    }

    // p is the handle to object
    public static long add
    (final IUnsafeAllocator allocator,
     final long list,
     final long p) {
        final ByteBuffer listBuffer = allocator.getByteBuffer(list);
        final int listBufferOffset = Utils.getBufferIndex(list);

        // Create a new node
        final long tmp = Node.create(allocator, p, Constants.NULL);

        // If list is not empty, put the new node as next of last node
        final long last = listBuffer.getLong(listBufferOffset + LAST_OFFSET);
        if (last != Constants.NULL) {
            Node.setNext(allocator, last, tmp);
        }

        // New node becomes the last node
        listBuffer.putLong(listBufferOffset + LAST_OFFSET, tmp);

        // If list is empty, set the first node to last one
        final long first = listBuffer.getLong(listBufferOffset + FIRST_OFFSET);
        if (first == Constants.NULL) {
            listBuffer.putLong(listBufferOffset + FIRST_OFFSET, tmp);
        }

        return list;
    }

    public static long getFirst
            (final IUnsafeAllocator allocator,
             final long list) {
        if (list == Constants.NULL)
            throw new NullPointerException("List is NULL");
        return allocator.getByteBuffer(list)
                .getLong(Utils.getBufferIndex(list) + FIRST_OFFSET);
    }

    public static long getNext
            (final IUnsafeAllocator allocator,
             final long node) {
        long next = Constants.NULL;

        if (node != Constants.NULL) {
            next = Node.getNext(allocator, node);
        }

        return next;
    }

    public static long getP
            (final IUnsafeAllocator allocator,
             final long node) {
        long p = Constants.NULL;

        if (node != Constants.NULL) {
            p = Node.getP(allocator, node);
        }

        return p;
    }
}
