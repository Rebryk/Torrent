package ru.spbau.mit;

import java.net.Socket;
import java.util.*;

/**
 * Created by rebryk on 13/04/16.
 */

public class ClientHandlerFactory implements HandlerFactory {
    private final List<FileShortDescription> files;
    private final Map<ClientDescription, Set<Integer>> clients;
    private final Map<ClientDescription, TimerTask> removeTasks;

    public ClientHandlerFactory(final List<FileShortDescription> files,
                                final Map<ClientDescription, Set<Integer>> clients,
                                final Map<ClientDescription, TimerTask> removeTasks) {
        this.files = files;
        this.clients = clients;
        this.removeTasks = removeTasks;
    }

    @Override
    public Runnable createHandler(Socket socket) {
        return new ClientHandler(socket, files, clients, removeTasks);
    }
}
