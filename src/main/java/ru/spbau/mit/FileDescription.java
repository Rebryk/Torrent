package ru.spbau.mit;

import java.io.File;
import java.nio.file.Path;
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
