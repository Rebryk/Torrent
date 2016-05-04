package ru.spbau.mit;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

/**
 * Created by rebryk on 13/04/16.
 */

public class P2PConnection extends Server {
    private Socket socket;
    // not nullable final fields
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    public P2PConnection(final Map<Integer, FileDescription> files) {
        super(0);
        setHandlerFactory(socket -> new P2PClientHandler(socket, files));
    }

    public void connect(final byte[] ip, final int port) throws IOException {
        socket = new Socket(InetAddress.getByAddress(ip), port);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
    }

    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public byte[] getFileBlock(final int fileId, final int block) throws IOException {
        if (outputStream == null || inputStream == null) {
            return null;
        }

        outputStream.writeInt(RequestType.GET_FILE_PART.getId());
        outputStream.writeInt(fileId);
        outputStream.writeInt(block);
        outputStream.flush();

        byte[] buffer = new byte[TorrentSettings.BLOCK_SIZE];
        inputStream.readFully(buffer);
        return buffer;
    }

    public List<Integer> getFileStatus(final int fileId) throws IOException {
        if (outputStream == null || inputStream == null) {
            return null;
        }

        outputStream.writeInt(RequestType.GET_FILE_STATUS.getId());
        outputStream.writeInt(fileId);
        outputStream.flush();

        final int count = inputStream.readInt();
        List<Integer> parts = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            parts.add(inputStream.readInt());
        }

        return parts;
    }
}
