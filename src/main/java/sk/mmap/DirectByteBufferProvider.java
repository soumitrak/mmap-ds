package sk.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DirectByteBufferProvider implements ByteBufferProvider {
    private static final Logger logger = LoggerFactory.getLogger(DirectByteBufferProvider.class);

    public ByteBuffer getNextBuffer() throws IOException {
        logger.debug("Allocating next byte buffer of size {}", Constants.MMAP_BUFFER_SIZE);
        return ByteBuffer.allocateDirect(Constants.DIRECT_BUFFER_SIZE);
    }

    public void close() {
    }

    public ByteBufferProvider debug() {
        return this;
    }
}
