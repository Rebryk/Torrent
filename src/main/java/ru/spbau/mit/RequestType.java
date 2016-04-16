package ru.spbau.mit;

/**
 * Created by rebryk on 14/04/16.
 */
public enum RequestType {
    GET_FILE_PART (0),
    GET_FILE_STATUS (1),
    GET_FILES_LIST (2),
    UPLOAD_FILE (3),
    GET_SEEDS (4),
    UPDATE (5),
    NONE (6);

    private final int value;
    RequestType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static RequestType get(final int request) {
        if (request < NONE.getValue()) {
            return values()[request];
        }
        return NONE;
    }
}
