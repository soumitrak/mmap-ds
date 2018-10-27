package sk.util;

import sk.mmap.Constants;
import sk.mmap.IUnsafeAllocator;
import sk.mmap.Utils;

import java.nio.ByteBuffer;

// Memory mapped linked list of Long - Unsafe
public class MULinkedListL {
    private static class Node {
        // Long value
        private static int L_SIZE = Constants.SIZE_OF_LONG;
        private static int L_OFFSET = 0;

        // Pointer to next Node
        private static int NP_SIZE = Constants.SIZE_OF_LONG;
        private static int NP_OFFSET = L_OFFSET + L_SIZE;

        private static int sizeof() {
            return L_SIZE + NP_SIZE;
        }

        private static long create
                (final IUnsafeAllocator allocator,
                 final long l,
                 final long np) {
            final long handle = allocator.alloc(sizeof());
            final int bufferOffset = Utils.getBufferIndex(handle);
            allocator.getByteBuffer(handle)
                    .putLong(bufferOffset + L_OFFSET, l)
                    .putLong(bufferOffset + NP_OFFSET, np);
            return handle;
        }

        private static long setL
                (final IUnsafeAllocator allocator,
                 final long handle,
                 final long l) {
            allocator.getByteBuffer(handle)
                    .putLong(Utils.getBufferIndex(handle) + L_OFFSET, l);
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

        private static long getL
                (final IUnsafeAllocator allocator,
                 final long handle) {
            return allocator.getByteBuffer(handle)
                    .getLong(Utils.getBufferIndex(handle) + L_OFFSET);
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

    // int ic - initial capacity, allocate space for 'ic' nodes during creation of list itself
    private static int IC_SIZE = Constants.SIZE_OF_INT;
    private static int IC_OFFSET = LAST_OFFSET + LAST_SIZE;

    // int uc - used capacity, used capacity out of initial capacity.
    private static int UC_SIZE = Constants.SIZE_OF_INT;
    private static int UC_OFFSET = IC_OFFSET + IC_SIZE;

    private static int sizeof() {
        return FIRST_SIZE + LAST_SIZE + IC_SIZE + UC_SIZE;
    }

    public static long create
            (final IUnsafeAllocator allocator) {
        return create(allocator, 0);
    }

    public static long create
            (final IUnsafeAllocator allocator,
             final int initialCapacity) {
        final long first = initialCapacity > 0 ?
                allocator.alloc(initialCapacity * Node.sizeof()) :
                Constants.NULL;

        final long list = allocator.alloc(sizeof());
        final int bufferOffset = Utils.getBufferIndex(list);

        allocator.getByteBuffer(list)
                .putLong(bufferOffset + FIRST_OFFSET, first)
                .putLong(bufferOffset + LAST_OFFSET, Constants.NULL)
                .putInt(bufferOffset + IC_OFFSET, initialCapacity)
                .putInt(bufferOffset + UC_OFFSET, 0);

        return list;
    }

    public static long add
            (final IUnsafeAllocator allocator,
             final long list,
             final long p) {
        final ByteBuffer listBuffer = allocator.getByteBuffer(list);
        final int listBufferOffset = Utils.getBufferIndex(list);

        final int ic = listBuffer.getInt(listBufferOffset + IC_OFFSET);
        int uc = listBuffer.getInt(listBufferOffset + UC_OFFSET);

        if (uc == ic) {
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
        } else {
            final long first = listBuffer.getLong(listBufferOffset + FIRST_OFFSET);
            final int arenaIndex = Utils.getArenaIndex(first);
            final int bufferIndex = Utils.getBufferIndex(first);

            // Get the pointer to node in the initialCapacity area
            final long node = Utils.getHandle(arenaIndex, bufferIndex + uc * Node.sizeof());
            Node.setL(allocator, node, p);
            Node.setNext(allocator, node, Constants.NULL);

            // If list is not empty, put the new node as next of last node
            final long last = listBuffer.getLong(listBufferOffset + LAST_OFFSET);
            if (last != Constants.NULL) {
                Node.setNext(allocator, last, node);
            }

            // New node becomes the last node
            listBuffer.putLong(listBufferOffset + LAST_OFFSET, node);

            // Increment the used capacity counter
            listBuffer.putInt(listBufferOffset + UC_OFFSET, ++uc);
        }

        return list;
    }

    public static long getFirst
            (final IUnsafeAllocator allocator,
             final long list) {
        if (list == Constants.NULL)
            throw new NullPointerException("List is NULL");

        final ByteBuffer listBuffer = allocator.getByteBuffer(list);
        final int listBufferOffset = Utils.getBufferIndex(list);

        long first = listBuffer.getLong(listBufferOffset + FIRST_OFFSET);

        final int ic = listBuffer.getInt(listBufferOffset + IC_OFFSET);
        final int uc = listBuffer.getInt(listBufferOffset + UC_OFFSET);
        if (ic > 0 && uc == 0) {
            first = Constants.NULL;
        }

        return first;
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

    public static long getL
            (final IUnsafeAllocator allocator,
             final long node) {
        long p = Constants.NULL;

        if (node != Constants.NULL) {
            p = Node.getL(allocator, node);
        }

        return p;
    }
}
