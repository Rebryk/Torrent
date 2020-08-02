package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by rebryk on 14/04/16.
 */
public abstract class SocketHandler implements Runnable {
    private final Socket socket;

    public SocketHandler(final Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            while (!socket.isClosed()) {
                handleRequest(inputStream, outputStream);
            }
        } catch (IOException | UnsupportedOperationException e) {
            // socket was closed
        }
    }

    protected Socket getSocket() {
        return socket;
    }

    protected abstract void handleRequest(final DataInputStream inputStream, final DataOutputStream outputStream)
            throws IOException, UnsupportedOperationException;
}
