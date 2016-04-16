package ru.spbau.mit;

import java.net.Socket;
import java.util.Map;

/**
 * Created by rebryk on 14/04/16.
 */
public class P2PClientHandlerFactory implements HandlerFactory {
    private final Map<Integer, FileDescription> files;

    public P2PClientHandlerFactory(final Map<Integer, FileDescription> files) {
        this.files = files;
    }

    @Override
    public Runnable createHandler(Socket socket) {
        return new P2PClientHandler(socket, files);
    }
}
