package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

/**
 * Created by rebryk on 13/04/16.
 */

public class P2PConnection extends AbstractServer {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private final Map<Integer, FileDescription> files; // local files

    public P2PConnection(final short port) {
        super(port);
        files = new HashMap<>();
        setHandlerFactory(new P2PClientHandlerFactory(files));
    }

    public void connect(final byte[] ip, final short port) throws IOException {
        socket = new Socket(InetAddress.getByAddress(ip), port);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
    }

    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public void addFile(final FileDescription file) {
        synchronized (files) {
            files.put(file.getId(), file);
        }
    }

    public void addBlock(final int fileId, final int block) {
        synchronized (files) {
            if (files.containsKey(fileId)) {
                files.get(fileId).addBlock(block);
            }
        }
    }

    public List<Integer> getAvailableFiles() {
        synchronized (files) {
            return new ArrayList<>(files.keySet());
        }
    }

    public byte[] getFileBlock(final int fileId, final int block) throws IOException {
        if (outputStream == null || inputStream == null) {
            return null;
        }

        outputStream.writeInt(RequestType.GET_FILE_PART.getValue());
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

        outputStream.writeInt(RequestType.GET_FILE_STATUS.getValue());
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
