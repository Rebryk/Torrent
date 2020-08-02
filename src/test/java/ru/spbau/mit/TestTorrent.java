package ru.spbau.mit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import org.apache.commons.io.FileUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by rebryk on 16/04/16.
 */

public class TestTorrent {
    private static final Path DIRECTORY_PATH = Paths.get("src", "test", "test_folder");

    private static final Path DIRECTORY_CLIENT_1 = DIRECTORY_PATH.resolve("client_1");
    private static final String FILE_NAME_1 = "File 1";
    private static final long FILE_SIZE_1 = TorrentSettings.BLOCK_SIZE + 1;
    private static final Path FILE_PATH_1 = DIRECTORY_CLIENT_1.resolve(FILE_NAME_1);

    private static final Path DIRECTORY_CLIENT_2 = DIRECTORY_PATH.resolve("client_2");
    private static final String FILE_NAME_2 = "File 2";
    private static final long FILE_SIZE_2 = 2 * TorrentSettings.BLOCK_SIZE;
    private static final Path FILE_PATH_2 = DIRECTORY_CLIENT_2.resolve(FILE_NAME_2);

    @Before
    public void createDirectory() throws IOException {
        Files.createDirectory(DIRECTORY_PATH);

        Files.createDirectory(DIRECTORY_CLIENT_1);
        createFile(FILE_PATH_1, FILE_SIZE_1);

        Files.createDirectory(DIRECTORY_CLIENT_2);
        createFile(FILE_PATH_2, FILE_SIZE_2);
    }

    @After
    public void removeDirectory() throws IOException {
        FileUtils.deleteDirectory(DIRECTORY_PATH.toFile());
    }

    @Test
    public void testSimple() throws InterruptedException, IOException {
        TorrentTrackerMain server = new TorrentTrackerMain();
        server.start();

        TorrentClientMain client1 = new TorrentClientMain();
        client1.start(TorrentSettings.SERVER_IP);
        client1.uploadFile(FILE_PATH_1);

        TorrentClientMain client2 = new TorrentClientMain();
        client2.start(TorrentSettings.SERVER_IP);

        final List<FileShortDescription> list1 = client2.getFilesList();
        client2.download(0, DIRECTORY_CLIENT_2);
        client2.uploadFile(FILE_PATH_2);

        final List<FileShortDescription> list2 = client1.getFilesList();

        final List<FileShortDescription> correctList1 =
                Arrays.asList(new FileShortDescription(0, FILE_NAME_1, FILE_SIZE_1));

        final List<FileShortDescription> correctList2 = Arrays.asList(
                new FileShortDescription(0, FILE_NAME_1, FILE_SIZE_1),
                new FileShortDescription(1, FILE_NAME_2, FILE_SIZE_2));

        assertEquals(list1, correctList1);
        assertEquals(list2, correctList2);
        assertEquals(FILE_SIZE_1, DIRECTORY_CLIENT_2.resolve(FILE_NAME_1).toFile().length());

        client1.stop();
        client2.stop();
        server.stop();
    }

    private void createFile(Path filePath, long fileSize) throws IOException {
        RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "rw");
        file.setLength(fileSize);
        file.close();
    }
}
