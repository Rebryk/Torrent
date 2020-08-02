package ru.spbau.mit;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rebryk on 13/04/16.
 */
public class TorrentClientMain {
    private int port;
    private final Map<Integer, FileDescription> files;
    private final Map<InetAddress, List<Integer>> filesToDownload;

    private P2PConnection connection;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private Timer updateTimer;
    private TimerTask updateTask;

    TorrentClientMain() {
        this.files = new ConcurrentHashMap<>();
        this.filesToDownload = new ConcurrentHashMap<>();
        this.connection = new P2PConnection(files);
    }

    public synchronized void start(final byte[] ip) throws IOException {
        updateTimer = new Timer();

        socket = new Socket(InetAddress.getByAddress(ip), TorrentSettings.TORRENT_SERVER_PORT);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());

        connection.start();
        port = connection.getPort();

        updateTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    update();
                } catch (IOException e) {
                    // update failed
                }
            }
        };

        updateTimer.schedule(updateTask, 0, TorrentSettings.UPDATE_DELAY);
    }

    public synchronized void stop() throws IOException {
        updateTimer.cancel();
        updateTimer.purge();
        updateTask.cancel();

        connection.stop();
        connection.disconnect();
        socket.close();
    }

    public void addFile(final FileDescription file) {
        files.put(file.getId(), file);
    }

    public void addBlock(final int fileId, final int block) {
        if (files.containsKey(fileId)) {
            files.get(fileId).addBlock(block);
        }
    }

    public List<Integer> getAvailableFiles() {
        return new ArrayList<>(files.keySet());
    }

    public synchronized List<FileShortDescription> getFilesList() throws IOException {
        System.out.println("client: getFilesList started.");

        outputStream.writeInt(RequestType.GET_FILES_LIST.getId());
        outputStream.flush();

        final int count = inputStream.readInt();
        List<FileShortDescription> files = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            files.add(new FileShortDescription(inputStream));
        }

        System.out.println("client: getFilesList finished.");
        return files;
    }

    public synchronized void uploadFile(final Path path) throws IOException {
        System.out.println("client: uploadFile started.");
        final File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new NoSuchFileException(path.toString());
        }

        outputStream.writeInt(RequestType.UPLOAD_FILE.getId());
        outputStream.writeUTF(file.getName());
        outputStream.writeLong(file.length());
        outputStream.flush();

        final int fileId = inputStream.readInt();
        addFile(new FileDescription(fileId, file));
        update();

        System.out.println("client: uploadFile finished.");
    }

    public synchronized void download(final int fileId, final Path path) throws IOException {
        final List<ClientDescription> seeds = getSeeds(fileId);
        final FileShortDescription fileShortDesc = getFileShortDescription(fileId);

        if (fileShortDesc == null) {
            return;
        }

        final Path filePath = path.resolve(fileShortDesc.getName());
        final RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "rw");

        final long fileSize = fileShortDesc.getSize();
        file.setLength(fileSize);

        final FileDescription fileDesc = new FileDescription(fileId, filePath, fileSize);
        addFile(fileDesc);

        while (!fileDesc.isDownloaded()) {
            for (ClientDescription seed : seeds) {
                try {
                    connection.connect(seed.getIp(), seed.getPort());
                } catch (IOException e) {
                    continue;
                }

                final BitSet blocks = fileDesc.getBlocks();
                for (int block = blocks.nextClearBit(0);
                     block < fileDesc.getBlocksCount();
                     block = blocks.nextClearBit(block + 1)) {

                    int blockSize = fileDesc.getBlockSize(block);
                    byte[] buffer;
                    try {
                        buffer = connection.getFileBlock(fileId, block);
                    } catch (IOException e) {
                        continue;
                    }

                    file.seek(block * TorrentSettings.BLOCK_SIZE);
                    file.write(buffer, 0, blockSize);

                    addBlock(fileId, block);
                }
                connection.disconnect();
            }
        }
        file.close();
    }

    public void saveFilesInfo() throws IOException {
        File dataFile = TorrentSettings.DATA_FILE_PATH.toFile();

        if (!dataFile.exists()) {
            Files.createFile(TorrentSettings.DATA_FILE_PATH);
            dataFile = TorrentSettings.DATA_FILE_PATH.toFile();
        }

        DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(dataFile));

        outputStream.writeInt(files.size());
        for (Map.Entry<Integer, FileDescription> entry : files.entrySet()) {
            entry.getValue().writeToStream(outputStream);
        }

        outputStream.writeInt(filesToDownload.size());
        for (Map.Entry<InetAddress, List<Integer>> entry : filesToDownload.entrySet()) {
            outputStream.write(entry.getKey().getAddress());
            List<Integer> fileIds = entry.getValue();
            outputStream.writeInt(fileIds.size());
            for (int id : fileIds) {
                outputStream.writeInt(id);
            }
        }

        outputStream.close();
    }

    public void loadFilesInfo() throws IOException {
        if (!TorrentSettings.DATA_FILE_PATH.toFile().exists()) {
            throw new NoSuchFileException(TorrentSettings.DATA_FILE_PATH.toString());
        }

        File dataFile = TorrentSettings.DATA_FILE_PATH.toFile();
        DataInputStream inputStream = new DataInputStream(new FileInputStream(dataFile));

        final int filesCount = inputStream.readInt();
        for (int i = 0; i < filesCount; i++) {
            final FileDescription file = new FileDescription(inputStream);
            files.put(file.getId(), file);
        }

        final int filesToDownloadCount = inputStream.readInt();
        for (int i = 0; i < filesToDownloadCount; ++i) {
            byte[] ip = new byte[TorrentSettings.IP_BUFFER_SIZE];
            inputStream.read(ip, 0, TorrentSettings.IP_BUFFER_SIZE);
            final List<Integer> fileIds = new ArrayList<>();
            final int listSize = inputStream.readInt();
            for (int j = 0; j < listSize; ++j) {
                fileIds.add(inputStream.readInt());
            }
            filesToDownload.put(InetAddress.getByAddress(ip), fileIds);
        }
    }

    public void addFileToDownload(final InetAddress address, final int id) {
        if (!filesToDownload.containsKey(address)) {
            filesToDownload.put(address, new ArrayList<>());
        }
        filesToDownload.get(address).add(id);
    }

    public void run(final byte[] address) throws IOException {
        final List<Integer> files = filesToDownload.get(InetAddress.getByAddress(address));
        if (files != null) {
            final Path downloadDirectory = Paths.get(TorrentSettings.DOWNLOAD_DIRECTORY);
            if (!downloadDirectory.toFile().exists()) {
                Files.createDirectories(downloadDirectory);
            }

            for (int file : files) {
                download(file, Paths.get(TorrentSettings.DOWNLOAD_DIRECTORY));
            }
        }

        while (!socket.isClosed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }
        System.out.println("TorrentClientMain: run finished!");
    }

    public static void main(String[] args) {
        TorrentClientMain client = new TorrentClientMain();

        try {
            client.loadFilesInfo();
        } catch (IOException e) {
            System.out.println("No data to load!");
        }

        byte[] address;
        try {
            address = InetAddress.getByName(args[1]).getAddress();
        } catch (UnknownHostException e) {
            System.out.println("Wrong tracker address!");
            return;
        }

        switch (args[0]) {
            case Console.RUN:
                System.out.println("client: run");
                Console.run(client, address);
                break;
            case Console.LIST:
                System.out.println("client: list");
                Console.list(client, address);
                break;
            case Console.DOWNLOAD:
                System.out.println("client: download");
                Console.download(client, address, Integer.parseInt(args[2]));
                break;
            case Console.UPLOAD:
                System.out.println("client: newfile");
                Console.upload(client, address, args[2]);
                break;
            default:
                System.out.println(Console.UNKNOWN_COMMAND);
        }


        System.out.println("Trying to save data...");
        try {
            client.saveFilesInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FileShortDescription getFileShortDescription(final int fileId) throws IOException {
        final List<FileShortDescription> files = getFilesList();
        for (FileShortDescription file : files) {
            if (file.getId() == fileId) {
                return file;
            }
        }
        return null;
    }

    private synchronized boolean update() throws IOException {
        final List<Integer> files = getAvailableFiles();

        outputStream.writeInt(RequestType.UPDATE.getId());
        outputStream.writeInt(port);
        outputStream.writeInt(files.size());
        for (Integer fileId : files) {
            outputStream.writeInt(fileId);
        }
        outputStream.flush();
        return inputStream.readBoolean();
    }

    private List<ClientDescription> getSeeds(final int fileId) throws IOException {
        outputStream.writeInt(RequestType.GET_SEEDS.getId());
        outputStream.writeInt(fileId);
        outputStream.flush();

        final int count = inputStream.readInt();
        List<ClientDescription> seeds = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            seeds.add(new ClientDescription(inputStream));
        }

        return seeds;
    }
}
