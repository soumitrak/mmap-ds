package sk.mmap;

import java.io.IOException;
import java.nio.channels.FileChannel;

public interface FileChannelProvider {
    FileChannel getFileChannel() throws IOException;

    void close();

    FileChannelProvider debug();
}
