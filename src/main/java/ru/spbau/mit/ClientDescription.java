package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by rebryk on 14/04/16.
 */
public class ClientDescription {
    private final byte[] ip;
    private final int port;

    public ClientDescription(final byte[] ip, final int port) {
        this.ip = ip;
        this.port = port;
    }

    public ClientDescription(final byte[] ip, final DataInputStream stream) throws IOException {
        this(ip, stream.readInt());
    }

    public ClientDescription(final DataInputStream stream) throws IOException {
        this.ip = new byte[TorrentSettings.IP_BUFFER_SIZE];
        stream.read(ip, 0, TorrentSettings.IP_BUFFER_SIZE);
        this.port = stream.readInt();
    }

    public byte[] getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        return port + 31 * Arrays.hashCode(ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientDescription)) {
            return false;
        }
        ClientDescription description = (ClientDescription) obj;
        return Arrays.equals(ip, description.ip) && port == description.port;
    }

    public void writeToStream(final DataOutputStream stream) throws IOException {
        stream.write(ip);
        stream.writeInt(port);
    }

}
