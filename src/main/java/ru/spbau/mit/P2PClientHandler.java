package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.BitSet;
import java.util.Map;

/**
 * Created by rebryk on 14/04/16.
 */

public class P2PClientHandler extends SocketHandler {
    private final Map<Integer, FileDescription> files;

    public P2PClientHandler(final Socket socket, final Map<Integer, FileDescription> files) {
        super(socket);
        this.files = files;
    }

    @Override
    protected void handleRequest(final DataInputStream inputStream, final DataOutputStream outputStream)
            throws IOException, UnsupportedOperationException {
        RequestType request = RequestType.get(inputStream.readInt());
        switch (request) {
            case GET_FILE_STATUS:
                getFileStatus(inputStream, outputStream);
                break;
            case GET_FILE_PART:
                getFileBlock(inputStream, outputStream);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void getFileStatus(final DataInputStream inputStream,
                               final DataOutputStream outputStream) throws IOException {
        final int fileId = inputStream.readInt();

        synchronized (files) {
            if (files.containsKey(fileId)) {
                BitSet parts = files.get(fileId).getBlocks();
                outputStream.writeInt(parts.cardinality());

                for (Integer part = 0; part < parts.length(); ++part) {
                    if (parts.get(part)) {
                        outputStream.writeInt(part);
                    }
                }
            } else {
                outputStream.writeInt(0);
            }
        }

        outputStream.flush();
    }

    private void getFileBlock(final DataInputStream inputStream,
                              final DataOutputStream outputStream) throws IOException {
        final int fileId = inputStream.readInt();
        final int fileBlock = inputStream.readInt();

        synchronized (files) {
            if (files.containsKey(fileId) && files.get(fileId).hasBlock(fileBlock)) {
                // RA file
                DataInputStream fileStream =
                        new DataInputStream(Files.newInputStream(files.get(fileId).getPath()));
                fileStream.skipBytes(fileBlock * TorrentSettings.BLOCK_SIZE);

                byte[] buffer = new byte[TorrentSettings.BLOCK_SIZE];
                try {
                    fileStream.readFully(buffer);
                } catch (EOFException e) {
                    // last block
                }

                fileStream.close();
                outputStream.write(buffer);
                outputStream.flush();
            }
        }
    }
}
