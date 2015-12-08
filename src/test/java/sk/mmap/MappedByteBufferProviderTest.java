package sk.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.TreeSet;

public class MappedByteBufferProviderTest {
    private static final Logger logger = LoggerFactory.getLogger(MappedByteBufferProviderTest.class);

    @Test
    public void testMaxBuffers() {
        Set<StandardOpenOption> options = new TreeSet<StandardOpenOption>();
        options.add(StandardOpenOption.CREATE);
        options.add(StandardOpenOption.TRUNCATE_EXISTING);
        options.add(StandardOpenOption.READ);
        options.add(StandardOpenOption.WRITE);

        FileSystemFileChannelProvider channelProvider =
                new FileSystemFileChannelProvider("mmap.dat", options);

        MappedByteBufferProvider byteBufferProvider =
                new MappedByteBufferProvider(channelProvider, FileChannel.MapMode.READ_WRITE);

        for (int i = 0; i < 10; i++) {
            try {
                byteBufferProvider.getNextBuffer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
