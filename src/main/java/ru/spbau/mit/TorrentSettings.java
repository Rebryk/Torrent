package ru.spbau.mit;

/**
 * Created by rebryk on 14/04/16.
 */
public final class TorrentSettings {
    public static final int KB = 1024;
    public static final int MB = 1024 * KB;
    public static final int BLOCK_SIZE = 10 * MB;
    public static final short TORRENT_SERVER_PORT = 8081;
    public static final int HASH_BASE = 37;
    public static final long UPDATE_DELAY = 60_000;
    public static final byte[] SERVER_IP = new byte[]{127, 0, 0, 1};
    public static final short IP_BUFFER_SIZE = 4;

    private TorrentSettings() {}
}
