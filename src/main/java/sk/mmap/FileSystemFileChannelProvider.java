package sk.mmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class FileSystemFileChannelProvider implements FileChannelProvider {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemFileChannelProvider.class);

    private final String fileName;
    private final Set<StandardOpenOption> options;
    private FileChannel fileChannel;

    public FileSystemFileChannelProvider
            (String fileName,
             Set<StandardOpenOption> options) {
        this.fileName = fileName;
        this.options = options;
        this.fileChannel = null;
    }

    public FileChannel getFileChannel() throws IOException {
        if (fileChannel == null) {
            Path path = FileSystems.getDefault().getPath(fileName);
            // Path path = FileSystems.getDefault().getPath("/dev/zero");
            fileChannel = FileChannel.open(path, options);

            // TODO:
            // new File(fileName).deleteOnExit();
            logger.info("Deleting file {} status {}", fileName, new File(fileName).delete());
        }

        return fileChannel;
    }

    public void close() {
        try {
            fileChannel.close();
            fileChannel = null;
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Add to function signature.
        }
    }

    public FileChannelProvider debug() {
        logger.debug("File name is {}.", fileName);
        return this;
    }
}
