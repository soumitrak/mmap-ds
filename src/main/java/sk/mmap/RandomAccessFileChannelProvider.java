package sk.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class RandomAccessFileChannelProvider implements FileChannelProvider {
    private static final Logger logger = LoggerFactory.getLogger(RandomAccessFileChannelProvider.class);

    private final String fileName;
    private final String mode;
    private FileChannel fileChannel;

    public RandomAccessFileChannelProvider(String fileName, String mode) {
        this.fileName = fileName;
        this.mode = mode;
        this.fileChannel = null;
    }

    public FileChannel getFileChannel() throws IOException {
        if (fileChannel == null) {
            fileChannel = new RandomAccessFile(fileName, mode).getChannel();
        }

        return fileChannel;
    }

    public void close() {
        try {
            fileChannel.close();
            fileChannel = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileChannelProvider debug() {
        logger.debug("File name {}, mode {}", fileName, mode);
        return this;
    }
}
