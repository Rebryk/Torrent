package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;

/**
 * Created by rebryk on 13/04/16.
 */
public class FileDescription extends FileShortDescription {
    private Path path;

    private BitSet blocks;
    private int blocksCount;

    public FileDescription(final int id, final Path path, final long size) {
        super(id, path.getFileName().toString(), size);
        this.path = path;
        this.blocks = new BitSet();
        this.blocksCount = (int) ((size + TorrentSettings.BLOCK_SIZE - 1) / TorrentSettings.BLOCK_SIZE);
    }

    public FileDescription(final int id, final File file) {
        this(id, file.toPath(), file.length());

        for (int i = 0; i < blocksCount; ++i) {
            this.blocks.set(i);
        }
    }

    public FileDescription(final DataInputStream stream) throws IOException {
        super(stream);
        this.path = Paths.get(stream.readUTF());
        this.blocks = new BitSet();
        this.blocksCount = (int) ((getSize() + TorrentSettings.BLOCK_SIZE - 1) / TorrentSettings.BLOCK_SIZE);

        int savedBlocksCount = stream.readInt();
        for (int j = 0; j < savedBlocksCount; j++) {
            blocks.set(stream.readInt());
        }
    }

    public void writeToStream(final DataOutputStream stream) throws IOException {
        super.writeToStream(stream);
        stream.writeUTF(path.toString());
        stream.writeInt(blocks.size());
        for (int i = 0; i < blocksCount; ++i) {
            if (blocks.get(i)) {
                stream.writeInt(i);
            }
        }
    }

    public boolean isDownloaded() {
        return blocks.cardinality() == blocksCount;
    }

    public Path getPath() {
        return path;
    }

    public void addBlock(final int block) {
        blocks.set(block);
    }

    public BitSet getBlocks() {
        return blocks;
    }

    public int getBlocksCount() {
        return blocksCount;
    }

    public boolean hasBlock(final int block) {
        return blocks.get(block);
    }

    public int getBlockSize(final int block) {
        int blockSize = TorrentSettings.BLOCK_SIZE;
        if (block == blocksCount - 1) {
            blockSize = (int) (getSize() % TorrentSettings.BLOCK_SIZE);
        }
        return blockSize;
    }
}
