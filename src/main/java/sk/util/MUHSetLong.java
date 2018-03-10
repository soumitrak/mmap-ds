package sk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.mmap.Constants;
import sk.mmap.IUnsafeAllocator;

import java.util.Map;
import java.util.HashMap;

// Memory mapped hash set of Long
public class MUHSetLong {
    private static final Logger logger = LoggerFactory.getLogger(MUHSetLong.class);

    private final int numBuckets;

    private final IUnsafeAllocator allocator;

    private final MBUArrayListL buckets;

    // TODO: Implement rehashing when table is full to avoid adding lots of elements in list
    public MUHSetLong(final IUnsafeAllocator allocator, final int numBuckets) {
        this.allocator = allocator;
        this.numBuckets = numBuckets;
        buckets = new MBUArrayListL(allocator, numBuckets);
        // Initialize all buckets with NULL
        for (int i = 0; i < numBuckets; i++) {
            buckets.add(Constants.NULL);
        }
    }

    public void debug() {
        logger.debug("Printing details of Hash table");
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
        if (create) {
            // logger.debug("Key " + key + " : Hash " + hash + " : index " + index);
        }
        long list = buckets.get(index);
        if (create && list == Constants.NULL) {
            list = MULinkedListL.create(allocator);
            buckets.put(index, list);
        }

        return list;
    }

    public void put(final long value) {
        long list = getBucket(value, true);
        MULinkedListL.add(allocator, list, value);
    }

    public long get(final long key) {
        long value = Constants.NULL;

        final long list = getBucket(key, false);
        if (list != Constants.NULL) {
            long node = MULinkedListL.getFirst(allocator, list);
            // If list is non-NULL, search in the MULinkedListL
            while (node != Constants.NULL) {

                final long tmp = MULinkedListL.getL(allocator, node);
                if (tmp == key) {
                    value = tmp;
                    break;
                }

                node = MULinkedListL.getNext(allocator, node);
            }
        }

        return value;
    }
}
