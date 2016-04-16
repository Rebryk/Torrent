package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by rebryk on 14/04/16.
 */
public class FileShortDescription {
    private int id;
    private String name;
    private long size;

    public FileShortDescription(final int id, final String name, final long size) {
        this.id = id;
        this.name = name;
        this.size = size;
    }

    public FileShortDescription(final int id, final DataInputStream stream) throws IOException {
        this(id, stream.readUTF(), stream.readLong());
    }

    public FileShortDescription(final DataInputStream stream) throws IOException {
        this(stream.readInt(), stream);
    }

    public void writeToStream(final DataOutputStream stream) throws IOException {
        stream.writeInt(id);
        stream.writeUTF(name);
        stream.writeLong(size);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileShortDescription)) {
            return false;
        }
        FileShortDescription fileInfo = (FileShortDescription) obj;
        return id == fileInfo.id && name.equals(fileInfo.name) && size == fileInfo.size;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * (int) Math.pow(TorrentSettings.HASH_BASE, 2)
                + id * TorrentSettings.HASH_BASE + (int) size;
    }
}
