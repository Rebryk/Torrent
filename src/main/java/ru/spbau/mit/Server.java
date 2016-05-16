package ru.spbau.mit;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rebryk on 13/04/16.
 */

public abstract class Server {
    private final int port;
    private HandlerFactory handlerFactory;

    private ServerSocket socket;
    private ExecutorService executor;

    public Server(final int port) {
        this.port = port;
    }

    public void setHandlerFactory(final HandlerFactory factory) {
        this.handlerFactory = factory;
    }

    public void start() {
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        executor = Executors.newCachedThreadPool();
        executor.execute(() -> {
            while (true) {
                synchronized (this) {
                    if (socket.isClosed()) {
                        break;
                    }
                }

                try {
                    executor.execute(handlerFactory.createHandler(socket.accept()));
                } catch (IOException e) {
                    //socket closed;
                }
            }
        });
    }

    public void stop() {
        synchronized (this) {
            if (socket == null || socket.isClosed()) {
                return;
            }

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            executor.shutdown();
        }
    }

    public synchronized int getPort() {
        if (socket == null || socket.isClosed()) {
            return 0;
        }
        return socket.getLocalPort();
    }
}
