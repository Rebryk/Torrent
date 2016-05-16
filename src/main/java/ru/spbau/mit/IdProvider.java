package ru.spbau.mit;

/**
 * Created by rebryk on 14/04/16.
 */
public final class IdProvider {
    private static IdProvider instance = null;
    private int id;

    private IdProvider() {
        this.id = 0;
    }

    public static int getNewId() {
        if (instance == null) {
            instance = new IdProvider();
        }
        return instance.id++;
    }
}
