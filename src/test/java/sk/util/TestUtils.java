package sk.util;

import sk.mmap.FileSystemFileChannelProvider;
import sk.mmap.MappedByteBufferProvider;

import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.TreeSet;

public class TestUtils {

    public static MappedByteBufferProvider getMappedByteBufferProvider(String fileName) {
        Set<StandardOpenOption> options = new TreeSet<StandardOpenOption>();
        options.add(StandardOpenOption.CREATE);
        options.add(StandardOpenOption.TRUNCATE_EXISTING);
        options.add(StandardOpenOption.READ);
        options.add(StandardOpenOption.WRITE);

        FileSystemFileChannelProvider channelProvider =
                new FileSystemFileChannelProvider(fileName, options);

        MappedByteBufferProvider byteBufferProvider =
                new MappedByteBufferProvider(channelProvider, FileChannel.MapMode.READ_WRITE);

        return byteBufferProvider;
    }

}
