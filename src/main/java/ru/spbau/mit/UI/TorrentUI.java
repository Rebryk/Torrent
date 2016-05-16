package ru.spbau.mit.UI;

import ru.spbau.mit.FileDescription;
import ru.spbau.mit.FileShortDescription;
import ru.spbau.mit.TorrentClientMain;
import ru.spbau.mit.TorrentSettings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * Created by rebryk on 15/05/16.
 */

public final class TorrentUI {
    private static TorrentClientMain client = new TorrentClientMain();
    private static Map<Integer, FileDescription> localFiles = client.getFiles();
    private static List<FileShortDescription> serverFiles = new ArrayList<>();

    private static Timer timer;
    private static TimerTask localFilesListUpdateTask;

    private static JFrame frame;
    private static JTable localFilesTable;
    private static JTable serverFilesTable;
    private static JButton uploadButton;
    private static JButton refreshButton;
    private static JButton downloadButton;

    private TorrentUI() {}

    public static void main(final String[] args) throws IOException {
        try {
            client.loadFilesInfo();
        } catch (IOException e) {
            // do nothing...
        }
        client.start(TorrentSettings.SERVER_IP);

        buildUI();

        localFilesListUpdateTask = new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    localFilesTable.updateUI();
                });
            }
        };

        timer = new Timer();
        timer.schedule(localFilesListUpdateTask, 0, 1000);
    }

    private static void buildUI() {
        frame = new JFrame("Torrent");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    localFilesListUpdateTask.cancel();
                    timer.cancel();
                    client.saveFilesInfo();
                    client.stop();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                super.windowClosing(e);
            }
        });
        frame.setSize(600, 600);
        frame.setResizable(false);

        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Local files", buildLocalFilesTab());
        tabbedPane.addTab("Server files", buildServerFilesTab());

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    private static JTable buildLocalFilesTable() {
        JTable table = new JTable(new AbstractTableModel() {
            private final String[] columnNames = new String[]{"File", "Size", "Status"};

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public int getRowCount() {
                return localFiles.size();
            }

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                final FileDescription file = localFiles.get(localFiles.keySet().toArray()[rowIndex]);

                if (file != null) {
                    switch (columnIndex) {
                        case 0:
                            return file.getName();
                        case 1:
                            return file.getSize();
                        case 2:
                            return file.getProgress();
                        default:
                            return null;
                    }
                }
                return null;
            }
        });
        table.getColumnModel().getColumn(2).setCellRenderer(new ProgressRender());

        table.getColumnModel().getColumn(1).setMinWidth(80);
        table.getColumnModel().getColumn(1).setMaxWidth(100);

        table.getColumnModel().getColumn(2).setMinWidth(100);
        table.getColumnModel().getColumn(2).setMaxWidth(150);

        return table;
    }

    private static JTable buildServerFilesTable() {
        final JTable table = new JTable(new AbstractTableModel() {
            private final String[] columnNames = new String[]{"File", "Size"};

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public int getRowCount() {
                return serverFiles.size();
            }

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (rowIndex < serverFiles.size()) {
                    final FileShortDescription file = serverFiles.get(rowIndex);
                    switch (columnIndex) {
                        case 0:
                            return file.getName();
                        case 1:
                            return file.getSize();
                        default:
                            return null;
                    }
                }
                return null;
            }
        });

        table.getColumnModel().getColumn(1).setMinWidth(80);
        table.getColumnModel().getColumn(1).setMaxWidth(100);

        return table;
    }

    private static JPanel buildLocalFilesTab() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        final JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
        toolbar.setFloatable(false);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));

        uploadButton = new JButton("Upload file");
        uploadButton.setMargin(new Insets(2, 2, 2, 2));
        uploadButton.addActionListener((event) -> {
            final JFileChooser fc = new JFileChooser();
            int ret = fc.showOpenDialog(frame);
            if (ret == JFileChooser.APPROVE_OPTION) {
                try {
                    client.uploadFile(fc.getSelectedFile().toPath());
                    localFilesTable.updateUI();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        toolbar.add(uploadButton);

        localFilesTable = buildLocalFilesTable();

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(localFilesTable), BorderLayout.CENTER);

        return panel;
    }

    private static void setButtonEnable(final boolean enabled) {
        uploadButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
    }

    private static void updateServerFilesList() throws IOException {
        serverFiles = client.getFilesList();
        for (Iterator<FileShortDescription> it = serverFiles.iterator(); it.hasNext();) {
            final FileShortDescription file = it.next();
            if (localFiles.get(file.getId()) != null) {
                it.remove();
            }
        }
    }

    private static JPanel buildServerFilesTab() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        final JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
        toolbar.setFloatable(false);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));

        refreshButton = new JButton("Refresh");
        refreshButton.setMargin(new Insets(2, 2, 2, 2));
        refreshButton.addActionListener((event) -> {
            try {
                updateServerFilesList();
                serverFilesTable.updateUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        downloadButton = new JButton("Download");
        downloadButton.setMargin(new Insets(2, 2, 2, 2));
        downloadButton.addActionListener((event) -> {
            final int row = serverFilesTable.getSelectedRow();
            if (row != -1) {
                setButtonEnable(false);
                final FileShortDescription file = serverFiles.get(row);
                new Thread(() -> {
                    try {
                        final Path downloadDirectory = Paths.get(TorrentSettings.DOWNLOAD_DIRECTORY);
                        if (!downloadDirectory.toFile().exists()) {
                            Files.createDirectories(downloadDirectory);
                        }

                        client.download(file.getId(), downloadDirectory);
                        updateServerFilesList();

                        SwingUtilities.invokeLater(() -> {
                            serverFilesTable.updateUI();
                            setButtonEnable(true);
                        });
                    } catch (IOException e) {
                        // fail to download file
                    }
                }).start();
            }
        });


        toolbar.add(refreshButton);
        toolbar.add(downloadButton);

        serverFilesTable = buildServerFilesTable();

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(serverFilesTable), BorderLayout.CENTER);

        return panel;
    }
}
