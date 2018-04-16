package sk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.mmap.Constants;
import sk.mmap.IUnsafeAllocator;

import java.util.HashMap;
import java.util.Map;


// Memory mapped hash table of any object which has a unique key (long type).
abstract public class MUHTObjLong {
    protected static final Logger logger = LoggerFactory.getLogger(MUHTObjLong.class);

    private final int numBuckets;

    private final IUnsafeAllocator allocator;

    private final MBUArrayListL buckets;

    // TODO: Implement rehashing when table is full to avoid adding lots of elements in list
    protected MUHTObjLong(final IUnsafeAllocator allocator, final int numBuckets) {
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

        long list = buckets.get(index);
        if (create && list == Constants.NULL) {
            list = MULinkedListL.create(allocator);
            buckets.put(index, list);
        }

        return list;
    }

    public final void put(final long obj) {
        put(getId(allocator, obj), obj);
    }

    public final void put(final long id, final long obj) {
        long list = getBucket(id, true);
        MULinkedListL.add(allocator, list, obj);
    }

    abstract protected long getId(final IUnsafeAllocator allocator, final long handle);

    // Get the object by id, returns the handle if found, else NULL
    public final long get(final long id) {
        long obj = Constants.NULL;

        final long list = getBucket(id, false);
        if (list != Constants.NULL) {
            long node = MULinkedListL.getFirst(allocator, list);
            // If list is non-NULL, search in the MULinkedListL
            while (node != Constants.NULL) {

                final long tmp = MULinkedListL.getL(allocator, node);
                if (getId(allocator, tmp) == id) {
                    obj = tmp;
                    break;
                }

                node = MULinkedListL.getNext(allocator, node);
            }
        }

        return obj;
    }
}

