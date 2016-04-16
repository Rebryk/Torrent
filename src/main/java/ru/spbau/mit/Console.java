package ru.spbau.mit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by rebryk on 16/04/16.
 */

public final class Console {
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String LIST = "list";
    public static final String DOWNLOAD = "download";
    public static final String UPLOAD = "upload";
    public static final String EXIT = "exit";

    public static final String WRONG_FORMAT = "Wrong request format!";
    public static final String UNKNOWN_COMMAND = "Unknown command!";

    public static final String OK = ": OK";
    public static final String FAIL = ": FAIL";

    private static final String NAME = "Name: ";
    private static final String SIZE = "Size: ";
    private static final String ID = "Id: ";

    private Console() {}

    public static void start(final TorrentClient client) {
        try {
            client.start(TorrentSettings.SERVER_IP);
            System.out.println(START + OK);
        } catch (IOException e) {
            System.out.println(START + FAIL);
            System.out.println(e.getMessage());
        }
    }

    public static void stop(final TorrentClient client) {
        try {
            client.stop();
            System.out.println(STOP + OK);
        } catch (IOException e) {
            System.out.println(STOP + FAIL);
            System.out.println(e.getMessage());
        }
    }

    public static void list(final TorrentClient client) {
        try {
            List<FileShortDescription> filesList = client.getFilesList();
            for (FileShortDescription file : filesList) {
                System.out.println(ID + file.getId() + " "
                        + NAME + file.getName() + " "
                        + SIZE + file.getSize());
            }
            System.out.println(LIST + OK);
        } catch (IOException e) {
            System.out.println(LIST + FAIL);
            System.out.println(e.getMessage());
        }
    }

    public static void download(final TorrentClient client, final List<String> arguments) {
        if (arguments.size() == 2) {
            try {
                client.download(Integer.valueOf(arguments.get(0)), Paths.get(arguments.get(1)));
                System.out.println(DOWNLOAD + OK);
            } catch (Exception e) {
                System.out.println(DOWNLOAD + FAIL);
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println(WRONG_FORMAT);
        }
    }

    public static void upload(final TorrentClient client, final List<String> arguments) {
        if (arguments.size() == 1) {
            try {
                Path path = Paths.get(arguments.get(0));
                client.uploadFile(path);
                System.out.println(UPLOAD + OK);
            } catch (Exception e) {
                System.out.println(UPLOAD + FAIL);
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println(WRONG_FORMAT);
        }
    }

    public static void exit(final TorrentClient client) {
        try {
            client.saveFilesInfo();
            System.out.println(EXIT + OK);
        } catch (IOException e) {
            System.out.println(EXIT + FAIL);
            System.out.println(e.getMessage());
        }
    }
}
