package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by rebryk on 13/04/16.
 */
public class FileDescription extends FileShortDescription {
    private Path path;
    private Set<Integer> blocks;
    private int blocksCount;

    public FileDescription(final int id, final Path path, final long size) {
        super(id, path.getFileName().toString(), size);
        this.path = path;
        this.blocks = new HashSet<>();
        this.blocksCount = (int) ((size + TorrentSettings.BLOCK_SIZE - 1) / TorrentSettings.BLOCK_SIZE);
    }

    public FileDescription(final int id, final File file) {
        this(id, file.toPath(), file.length());

        for (int i = 0; i < blocksCount; ++i) {
            this.blocks.add(i);
        }
    }

    public FileDescription(final DataInputStream stream) throws IOException {
        super(stream);
        this.path = Paths.get(stream.readUTF());
        this.blocks = new HashSet<>();
        this.blocksCount = (int) ((getSize() + TorrentSettings.BLOCK_SIZE - 1) / TorrentSettings.BLOCK_SIZE);

        int savedBlocksCount = stream.readInt();
        for (int j = 0; j < savedBlocksCount; j++) {
            blocks.add(stream.readInt());
        }
    }

    public void writeToStream(final DataOutputStream stream) throws IOException {
        super.writeToStream(stream);
        stream.writeUTF(path.toString());
        stream.writeInt(blocks.size());
        for (int block : blocks) {
            stream.writeInt(block);
        }
    }

    public boolean isDownloaded() {
        return blocks.size() == blocksCount;
    }

    public Path getPath() {
        return path;
    }

    public void addBlock(final int block) {
        blocks.add(block);
    }

    public Set<Integer> getBlocks() {
        return blocks;
    }

    public int getBlocksCount() {
        return blocksCount;
    }

    public boolean hasBlock(final int block) {
        return blocks.contains(block);
    }
}
