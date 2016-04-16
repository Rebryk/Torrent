package ru.spbau.mit;

import java.util.*;

/**
 * Created by rebryk on 14/04/16.
 */
public class TorrentServer extends AbstractServer {
    public TorrentServer() {
        super(TorrentSettings.TORRENT_SERVER_PORT);

        List<FileShortDescription> files = Collections.synchronizedList(new ArrayList<>());
        Map<ClientDescription, Set<Integer>> clients = Collections.synchronizedMap(new HashMap<>());
        Map<ClientDescription, TimerTask> removeTasks = Collections.synchronizedMap(new HashMap<>());

        setHandlerFactory(new ClientHandlerFactory(files, clients, removeTasks));
    }

    public static void main(final String[] args) {
        final TorrentServer server = new TorrentServer();
        server.start();
    }
}
