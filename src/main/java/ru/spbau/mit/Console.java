package ru.spbau.mit;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by rebryk on 16/04/16.
 */

public final class Console {
    public static final String RUN = "run";
    public static final String LIST = "list";
    public static final String DOWNLOAD = "get";
    public static final String UPLOAD = "newfile";

    public static final String UNKNOWN_COMMAND = "Unknown command!";

    private static final String NAME = "Name: ";
    private static final String SIZE = "Size: ";
    private static final String ID = "Id: ";

    private Console() {}

    public static void run(final TorrentClientMain client, final byte[] address) {
        try {
            client.start(address);
            client.run(address);
        } catch (IOException e) {
            System.out.println("Console: run failed.");
            System.out.println(e.getMessage());
        }
    }

    public static void list(final TorrentClientMain client, final byte[] address) {
        try {
            client.start(address);
            List<FileShortDescription> filesList = client.getFilesList();

            if (filesList.isEmpty()) {
                System.out.println("No files!");
            } else {
                for (FileShortDescription file : filesList) {
                    System.out.println(ID + file.getId() + " "
                            + NAME + file.getName() + " "
                            + SIZE + file.getSize());
                }
            }

            client.stop();
        } catch (IOException e) {
            System.out.println("Console: list failed.");
            System.out.println(e.getMessage());
        }
    }

    public static void download(final TorrentClientMain client, final byte[] address, final int id) {
        try {
            client.addFileToDownload(InetAddress.getByAddress(address), id);
        } catch (Exception e) {
            System.out.println("Console: download failed.");
            System.out.println(e.getMessage());
        }
    }

    public static void upload(final TorrentClientMain client, final byte[] address, final String path) {
        try {
            client.start(address);
            client.uploadFile(Paths.get(path));
            client.stop();
        } catch (Exception e) {
            System.out.println("Console: upload failed.");
            System.out.println(e.getMessage());
        }
    }
}
