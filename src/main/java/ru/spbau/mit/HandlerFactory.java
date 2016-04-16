package ru.spbau.mit;

import java.net.Socket;

/**
 * Created by rebryk on 13/04/16.
 */

public interface HandlerFactory {
    Runnable createHandler(final Socket socket);
}
