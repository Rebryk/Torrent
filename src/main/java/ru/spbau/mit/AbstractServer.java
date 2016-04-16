package ru.spbau.mit;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rebryk on 13/04/16.
 */

public abstract class AbstractServer implements Server {
    private final short port;
    private HandlerFactory handlerFactory;

    private ServerSocket socket;
    private ExecutorService executor;

    public AbstractServer(final short port) {
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
                    executor.submit(handlerFactory.createHandler(socket.accept()));
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
}
