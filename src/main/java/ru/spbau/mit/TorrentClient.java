package ru.spbau.mit;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by rebryk on 13/04/16.
 */
public class TorrentClient {
    private final short port;

    private P2PConnection connection;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private Timer updateTimer;
    private TimerTask updateTask;

    TorrentClient(final short port) {
        this.port = port;
        this.connection = new P2PConnection(port);

        this.updateTimer = new Timer();
    }

    public void start(final byte[] ip) throws IOException {
        socket = new Socket(InetAddress.getByAddress(ip), TorrentSettings.TORRENT_SERVER_PORT);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());

        connection.start();

        updateTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    update();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        updateTimer.schedule(updateTask, 0, TorrentSettings.UPDATE_DELAY);
    }

    public void stop() throws IOException {
        updateTask.cancel();
        connection.stop();
        connection.disconnect();
        socket.close();
    }

    public List<FileShortDescription> getFilesList() throws IOException {
        outputStream.writeInt(RequestType.GET_FILES_LIST.getValue());
        outputStream.flush();

        final int count = inputStream.readInt();
        List<FileShortDescription> files = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            files.add(new FileShortDescription(inputStream));
        }

        return files;
    }

    public void uploadFile(final Path path) throws IOException {
        final File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new NoSuchFileException(path.toString());
        }

        outputStream.writeInt(RequestType.UPLOAD_FILE.getValue());
        outputStream.writeUTF(file.getName());
        outputStream.writeLong(file.length());
        outputStream.flush();

        final int fileId = inputStream.readInt();
        connection.addFile(new FileDescription(fileId, file));
        update();
    }

    public void download(final int fileId, final Path path) throws IOException {
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
        connection.addFile(fileDesc);

        while (!fileDesc.isDownloaded()) {
            for (ClientDescription seed : seeds) {
                connection.connect(seed.getIp(), seed.getPort());
                final List<Integer> blocks = connection.getFileStatus(fileId);
                for (int block : blocks) {
                    if (!fileDesc.hasBlock(block)) {
                        int blockSize = TorrentSettings.BLOCK_SIZE;
                        if (block == fileDesc.getBlocksCount() - 1) {
                            blockSize = (int) (fileSize % TorrentSettings.BLOCK_SIZE);
                        }

                        byte[] buffer;
                        try {
                            buffer = connection.getFileBlock(fileId, block);
                        } catch (IOException e) {
                            continue;
                        }

                        file.seek(block * TorrentSettings.BLOCK_SIZE);
                        file.write(buffer, 0, blockSize);

                        connection.addBlock(fileId, block);
                    }
                }
            }
        }
        file.close();
    }

    public void saveFilesInfo() throws IOException {
        connection.saveFilesInfo();
    }

    public void loadFilesInfo() throws IOException {
        connection.loadFilesInfo();
    }

    public static void main(String[] args) {
        TorrentClient client = new TorrentClient(Short.valueOf(args[0]));
        //TorrentClient client = new TorrentClient((short) 1025);

        try {
            client.loadFilesInfo();
        } catch (IOException e) {
            System.out.println("No data to load!");
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            List<String> arguments = Arrays.asList(line.split(" "));
            if (arguments.size() == 0) {
                continue;
            }

            switch (arguments.get(0)) {
                case Console.START:
                    Console.start(client);
                    break;
                case Console.STOP:
                    Console.stop(client);
                    break;
                case Console.LIST:
                    Console.list(client);
                    break;
                case Console.DOWNLOAD:
                    Console.download(client, arguments.subList(1, arguments.size()));
                    break;
                case Console.UPLOAD:
                    Console.upload(client, arguments.subList(1, arguments.size()));
                    break;
                case Console.EXIT:
                    Console.exit(client);
                    return;
                default:
                    System.out.println(Console.UNKNOWN_COMMAND);
            }
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

    private boolean update() throws IOException {
        final List<Integer> files = connection.getAvailableFiles();

        outputStream.writeInt(RequestType.UPDATE.getValue());
        outputStream.writeShort(port);
        outputStream.writeInt(files.size());
        for (Integer fileId : files) {
            outputStream.writeInt(fileId);
        }
        outputStream.flush();

        return inputStream.readBoolean();
    }

    private List<ClientDescription> getSeeds(final int fileId) throws IOException {
        outputStream.writeInt(RequestType.GET_SEEDS.getValue());
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
