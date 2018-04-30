package sk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.mmap.Utils;
import sk.mmap.Constants;
import sk.mmap.IUnsafeAllocator;

import java.util.Map;
import java.util.HashMap;


// Memory mapped hash table of long to long
final public class MUHTLong2Long {
    protected static final Logger logger = LoggerFactory.getLogger(MUHTLong2Long.class);

    private final int numBuckets;

    private final IUnsafeAllocator allocator;

    private final MBUArrayListL buckets;

    // TODO: Implement rehashing when table is full to avoid adding lots of elements in list
    public MUHTLong2Long(final IUnsafeAllocator allocator, final int numBuckets) {
        this.allocator = allocator;
        this.numBuckets = numBuckets;
        buckets = new MBUArrayListL(allocator, numBuckets);
        // Initialize all buckets with NULL
        for (int i = 0; i < numBuckets; i++) {
            buckets.add(Constants.NULL);
        }
    }

    public void debug() {
        logger.debug("Printing details of Hash table, num buckets {}", buckets.size());
        final Map<Integer, Integer> elems2buckets = new HashMap<>();
        for (int i = 0; i < buckets.size(); i++) {
            final long list = buckets.get(i);
            int elems = 0;
            if (list != Constants.NULL) {
                long node = MULinkedListL.getFirst(allocator, list);
                // If list is non-NULL, search in the MULinkedListL
                while (node != Constants.NULL) {
                    ++elems;
                    node = MULinkedListL.getNext(allocator, node);
                }
                /*
                if (elems > 1) {
                    logger.debug("Bucket " + i + " has " + elems + " elements");
                }
                */
            }

            if (elems2buckets.containsKey(elems)) {
                elems2buckets.put(elems, elems2buckets.get(elems) + 1);
            } else {
                elems2buckets.put(elems, 1);
            }
        }

        elems2buckets.forEach((key, val) -> {
            logger.debug(val + " buckets have " + key + " elements");
        });
        logger.debug("End printing details of Hash table");
    }

    private long getBucket(final long key, final boolean create) {
        final long hash = JenkinsHash.hash64(key);
        final int index =  (int)(hash % numBuckets);

        long list = buckets.get(index);
        if (create && list == Constants.NULL) {
            list = MULinkedListL.create(allocator);
            buckets.put(index, list);
        }

        return list;
    }

    public final void put(final long key, final long value) {
        long list = getBucket(key, true);
        final long obj = Pair.create(allocator, key, value);
        MULinkedListL.add(allocator, list, obj);
    }

    // Get the object by id, returns the handle if found, else NULL
    public final long get(final long id) {
        long obj = Constants.NULL;

        final long list = getBucket(id, false);
        if (list != Constants.NULL) {
            long node = MULinkedListL.getFirst(allocator, list);
            // If list is non-NULL, search in the MULinkedListL
            while (node != Constants.NULL) {

                final long handle = MULinkedListL.getL(allocator, node);
                if (Pair.getKey(allocator, handle) == id) {
                    obj = Pair.getValue(allocator, handle);
                    break;
                }

                node = MULinkedListL.getNext(allocator, node);
            }
        }

        return obj;
    }

    private static final class Pair {

        private static int key_SIZE = Constants.SIZE_OF_LONG;
        private static int key_OFFSET = 0;

        private static int value_SIZE = Constants.SIZE_OF_LONG;
        private static int value_OFFSET = key_OFFSET + key_SIZE;

        private static int sizeof() {
            return key_SIZE + value_SIZE;
        }

        static long create
                (final IUnsafeAllocator allocator,
                 final long key,
                 final long value) {
            final long handle = allocator.alloc(sizeof());
            final int bufferOffset = Utils.getBufferIndex(handle);
            allocator.getByteBuffer(handle)
                    .putLong(bufferOffset + key_OFFSET, key)
                    .putLong(bufferOffset + value_OFFSET, value);
            return handle;
        }

        static long getKey
                (final IUnsafeAllocator allocator,
                 final long handle) {
            return allocator.getByteBuffer(handle)
                    .getLong(Utils.getBufferIndex(handle) + key_OFFSET);
        }

        static long getValue
                (final IUnsafeAllocator allocator,
                 final long handle) {
            return allocator.getByteBuffer(handle)
                    .getLong(Utils.getBufferIndex(handle) + value_OFFSET);
        }
    }

}

