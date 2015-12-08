package sk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import sk.mmap.*;

import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class MArrayListITest {
    private static final Logger logger = LoggerFactory.getLogger(MArrayListITest.class);

    private void testArrayList(int count) {
        logger.info("Testing arraylist");
        ArrayList<Integer> list = new ArrayList<Integer>();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            list.add(random.nextInt());
        }
        logger.info("Iterating array now.");
        for (int i = 0; i < list.size(); ++i)
            list.get(i);
        logger.info("Done testing arraylist.");
    }

    @Test
    public void testMappedByteBuffer() {
        Set<StandardOpenOption> options = new TreeSet<StandardOpenOption>();
        options.add(StandardOpenOption.CREATE);
        options.add(StandardOpenOption.TRUNCATE_EXISTING);
        options.add(StandardOpenOption.READ);
        options.add(StandardOpenOption.WRITE);

        FileSystemFileChannelProvider channelProvider =
                new FileSystemFileChannelProvider("mmap.dat", options);

        MappedByteBufferProvider byteBufferProvider =
                new MappedByteBufferProvider(channelProvider, FileChannel.MapMode.READ_WRITE);

        testByteBuffer(byteBufferProvider);
    }

    private void testByteBuffer(ByteBufferProvider byteBufferProvider) {
        Mallocator mallocator = new Mallocator(byteBufferProvider);

        int initialCapacity = 100;
        int totalCapacity = 1000000 * 400;
        MArrayListI list = new MArrayListI(mallocator, initialCapacity);
        new MArrayListI(mallocator, initialCapacity).delete();
        new MArrayListI(mallocator, initialCapacity).delete();
        new MArrayListI(mallocator, 1000).delete();
        new MArrayListI(mallocator, 99).delete();
        new MArrayListI(mallocator, 1).delete();

        logger.info("Adding {} random int into array.", totalCapacity);
        Random random = new Random();
        MessageDigest pre = Hash.getDigest();
        for (int i = 0; i < totalCapacity; i++) {
            int j = random.nextInt();
            // Hash.update(pre, j);
            list.put(j);
            // list.put(i);
        }

        String iHash = Hash.hash(pre);
        logger.info("Input hash is " + iHash);

        MessageDigest post = Hash.getDigest();
        logger.info("Iterating array now.");
        for (int i = 0; i < list.size(); ++i) {
            int j = list.get(i);
            // Hash.update(post, j);
            // logger.info("{}th element is {}", i, j);
        }

        String oHash = Hash.hash(post);
        logger.info("Output hash is " + oHash);
        Assert.assertEquals(iHash, oHash);

        list.delete();
        mallocator.debug();
        mallocator.close();
        logger.info("Done testing MListi.");

        // testArrayList(totalCapacity);
    }

    @Test
    public void testDirectByteBuffer() {
        testByteBuffer(new DirectByteBufferProvider());
    }
}
