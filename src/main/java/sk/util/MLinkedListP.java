package sk.util;

import sk.mmap.Constants;
import sk.mmap.IAllocator;

// Memory mapped linked list of pointers
public class MLinkedListP {
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
                (final IAllocator allocator,
                 final long p,
                 final long np) {
            final long handle = allocator.alloc(sizeof());
            allocator.getByteBuffer(handle)
                    .putLong(P_OFFSET, p)
                    .putLong(NP_OFFSET, np);
            return handle;
        }

        private static long setNext
                (final IAllocator allocator,
                 final long handle,
                 final long np) {
            allocator.getByteBuffer(handle)
                    .putLong(NP_OFFSET, np);
            return handle;
        }

        private static long getP
                (final IAllocator allocator,
                 final long handle) {
            return allocator.getByteBuffer(handle)
                    .getLong(P_OFFSET);
            }

        private static long getNext
                (final IAllocator allocator,
                 final long handle) {
            return allocator.getByteBuffer(handle)
                    .getLong(NP_OFFSET);
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
            (final IAllocator allocator) {
        final long list = allocator.alloc(sizeof());
        allocator.getByteBuffer(list)
                .putLong(FIRST_OFFSET, Constants.NULL)
                .putLong(LAST_OFFSET, Constants.NULL);
        return list;
    }

    // p is the handle to object
    public static long add
        (final IAllocator allocator,
         final long list,
         final long p) {
        // Create a new node
        final long tmp = Node.create(allocator, p, Constants.NULL);

        // If list is not empty, put the new node as next of last node
        final long last = getLast(allocator, list);
        if (last != Constants.NULL) {
            Node.setNext(allocator, last, tmp);
        }

        // New node becomes the last node
        putLast(allocator, list, tmp);

        // If list is empty, set the first node to last one
        final long first = getFirst(allocator, list);
        if (first == Constants.NULL) {
            putFirst(allocator, list, tmp);
        }

        return list;
    }

    public static long getFirst
            (final IAllocator allocator,
             final long list) {
        if (list == Constants.NULL)
            throw new NullPointerException("List is NULL");
        return allocator.getByteBuffer(list).getLong(FIRST_OFFSET);
    }

    public static long getLast
            (final IAllocator allocator,
             final long list) {
        return allocator.getByteBuffer(list).getLong(LAST_OFFSET);
    }

    private static long putFirst
            (final IAllocator allocator,
             final long list,
             final long first) {
        allocator.getByteBuffer(list).putLong(FIRST_OFFSET, first);
        return list;
    }

    private static long putLast
            (final IAllocator allocator,
             final long list,
             final long last) {
        allocator.getByteBuffer(list).putLong(LAST_OFFSET, last);
        return list;
    }

    public static long getNext
            (final IAllocator allocator,
             final long node) {
        long next = Constants.NULL;

        if (node != Constants.NULL) {
            next = Node.getNext(allocator, node);
        }

        return next;
    }

    public static long getP
            (final IAllocator allocator,
             final long node) {
        long p = Constants.NULL;

        if (node != Constants.NULL) {
            p = Node.getP(allocator, node);
        }

        return p;
    }
}
