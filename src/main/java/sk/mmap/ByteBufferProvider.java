package sk.mmap;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ByteBufferProvider {
    ByteBuffer getNextBuffer() throws IOException;

    void close();

    ByteBufferProvider debug();
}
