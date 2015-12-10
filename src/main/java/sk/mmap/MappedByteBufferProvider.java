package sk.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MappedByteBufferProvider implements ByteBufferProvider {
    private static final Logger logger = LoggerFactory.getLogger(MappedByteBufferProvider.class);

    private final FileChannelProvider fileChannelProvider;
    private final FileChannel.MapMode mode;

    public MappedByteBufferProvider
            (FileChannelProvider fileChannelProvider,
             FileChannel.MapMode mode) {
        this.fileChannelProvider = fileChannelProvider;
        this.mode = mode;
    }

    public ByteBuffer getNextBuffer() throws IOException {
        logger.debug("Getting next mmap buffer of size {}", Constants.MMAP_BUFFER_SIZE);
        FileChannel fileChannel = fileChannelProvider.getFileChannel();

        long currentSize = fileChannel.size();
        long newSize = currentSize + Constants.MMAP_BUFFER_SIZE;

        // TODO: Not sure if this is required.
        logger.info("Truncating file from {} to {} size.", currentSize, newSize);
        fileChannel.truncate(newSize);

        return fileChannel.map(mode, currentSize, Constants.MMAP_BUFFER_SIZE);
    }

    public void close() {
        fileChannelProvider.close();
    }

    public ByteBufferProvider debug() {
        fileChannelProvider.debug();
        return this;
    }
}
