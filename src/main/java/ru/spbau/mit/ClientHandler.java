package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * Created by rebryk on 13/04/16.
 */
public class ClientHandler extends SocketHandler {
    private final List<FileShortDescription> files;
    private final Map<ClientDescription, Set<Integer>> clients;
    private final Map<ClientDescription, TimerTask> removeTasks;
    private final Timer removeTimer;

    public ClientHandler(final Socket socket,
                         final List<FileShortDescription> files,
                         final Map<ClientDescription, Set<Integer>> clients,
                         final Map<ClientDescription, TimerTask> removeTasks) {
        super(socket);
        this.files = files;
        this.clients = clients;
        this.removeTasks = removeTasks;
        this.removeTimer = new Timer();
    }

    @Override
    protected void handleRequest(final DataInputStream inputStream, final DataOutputStream outputStream)
            throws IOException, UnsupportedOperationException {
        RequestType request = RequestType.get(inputStream.readInt());
        switch (request) {
            case GET_FILES_LIST:
                getFilesList(outputStream);
                break;
            case UPLOAD_FILE:
                uploadFile(inputStream, outputStream);
                break;
            case GET_SEEDS:
                getSeeds(inputStream, outputStream);
                break;
            case UPDATE:
                update(inputStream, outputStream);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void getFilesList(final DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(files.size());
        for (FileShortDescription file : files) {
            file.writeToStream(outputStream);
        }
        outputStream.flush();
    }

    private void uploadFile(final DataInputStream inputStream,
                            final DataOutputStream outputStream) throws IOException {
        final FileShortDescription file = new FileShortDescription(IdProvider.getNewId(), inputStream);
        files.add(file);
        outputStream.writeInt(file.getId());
        outputStream.flush();
    }

    private void getSeeds(final DataInputStream inputStream,
                          final DataOutputStream outputStream) throws IOException {
        final int fileId = inputStream.readInt();
        final List<ClientDescription> seedingClients = new ArrayList<>();
        synchronized (clients) {
            clients.forEach((client, files) -> {
                if (files.contains(fileId)) {
                    seedingClients.add(client);
                }
            });
        }

        outputStream.writeInt(seedingClients.size());
        for (ClientDescription client : seedingClients) {
            client.writeToStream(outputStream);
        }
        outputStream.flush();
    }

    private void update(final DataInputStream inputStream,
                        final DataOutputStream outputStream) throws IOException {
        try {
            final byte[] address = getSocket().getInetAddress().getAddress();
            final ClientDescription client = new ClientDescription(address, inputStream);

            final int count = inputStream.readInt();
            Set<Integer> clientFiles = new HashSet<>();
            for (int i = 0; i < count; ++i) {
                clientFiles.add(inputStream.readInt());
            }

            if (removeTasks.containsKey(client)) {
                removeTasks.get(client).cancel();
            }

            TimerTask removeTask = new TimerTask() {
                @Override
                public void run() {
                    clients.remove(client);
                }
            };

            removeTasks.remove(client);
            removeTasks.put(client, removeTask);
            removeTimer.schedule(removeTask, TorrentSettings.UPDATE_DELAY);
            clients.remove(client);
            clients.put(client, clientFiles);
            outputStream.writeBoolean(true);
        } catch (IOException e) {
            outputStream.writeBoolean(false);
        }
        outputStream.flush();
    }
}
