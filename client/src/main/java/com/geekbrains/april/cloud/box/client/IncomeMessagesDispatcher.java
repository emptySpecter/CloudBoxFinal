package com.geekbrains.april.cloud.box.client;

import com.geekbrains.april.cloud.box.common.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IncomeMessagesDispatcher implements Runnable {

    private CloudBoxClient application;
    private MainController mainController;
    private int chunksize = 0;
    private String root_dir = "";

    private byte[] buffer;

    private Session session = new Session() {

        @Override
        public FileInfo getSavedFileInfo(FileInfo fileInfo) {
            if (!Files.exists(Paths.get(getFullCodedFileName(fileInfo)))) return null;
            return fileInfo;
        }

        @Override
        public void createAndSaveFileInfo(FileInfo fileInfo) throws IOException {
            Files.createFile(Paths.get(getFullCodedFileName(fileInfo)));
        }

        @Override
        public String getFullFileName(FileInfo fileInfo) {
            return root_dir + "/" + fileInfo.fileName;
        }

        @Override
        public String getFullCodedFileName(FileInfo fileInfo) {
            return root_dir + "/" + FXFileInfo.FileInfoToFileName(fileInfo);
        }

        @Override
        public void updateIndiviualSend(FileInfo info) throws CloneNotSupportedException {
            mainController.updateRemoteFileInfo((FileInfo) info.clone());
        }

        @Override
        public void updateListSend() {
            FXHelper.updateUI(() -> mainController.refreshLocalFilesList());
        }

        @Override
        public void updateIndiviualReceive(FileInfo info) throws CloneNotSupportedException {
            mainController.updateLocalFileInfo((FileInfo) info.clone());
        }

        @Override
        public AbstractMessage updateListReceive() {
            FXHelper.updateUI(() -> mainController.refreshLocalFilesList());
            return new FileListRequest();
        }

    };

    public IncomeMessagesDispatcher(CloudBoxClient application) {
        this.application = application;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        mainController.setCallbacks(
                x -> session.chunksSenderHashMap.containsKey(x),
                x -> session.chunksReceiverHashMap.containsKey(x),
                y -> session.StartStopToggle(y)
        );
    }

    public void setChunksize(int chunksize) {
        this.chunksize = chunksize;
        buffer = new byte[chunksize];
    }

    public void setRoot_dir(String root_dir) {
        this.root_dir = root_dir;
    }

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        try {
            while (!thread.isInterrupted()) {
                AbstractMessage am = Network.readObject();

                if (am instanceof FileChunkMessage) {
                    AbstractMessage result = session.Receive(((FileChunkMessage) am).getFileInfo(), ((FileChunkMessage) am).getData());
                    if (result != null) Network.sendMsg(result);
                }

                if (am instanceof FileDownloadRequest) {
                    AbstractMessage result = session.Send(((FileDownloadRequest) am).getFileInfo(), buffer);
                    if (result != null) Network.sendMsg(result);
                }

                if (am instanceof FileListMessage) FileList((FileListMessage) am);

                if (am instanceof InfoMessage) Info((InfoMessage) am);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        } finally {
        }
    }

    private void FileList(FileListMessage am) {
        FXHelper.updateUI(() -> mainController.refreshRemoteFilesList(am.getList()));
    }

    private void Info(InfoMessage am) {
        if (am.getCode() == InfoMessage.MessageCode.AUTHORIZATION_SUCCESSFUL) {
            Platform.runLater(() -> application.onAuthorizationAnswer(true));
        } else if (am.getCode() == InfoMessage.MessageCode.AUTHORIZATION_FAILED) {
            Platform.runLater(() -> application.onAuthorizationAnswer(false));
        } else if (am.getCode() == InfoMessage.MessageCode.FILE_CORRUPTED) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Uploaded file \"" + am.getInfo2() + "\" corrupted\nMD5: " + am.getInfo1(), ButtonType.OK);
                alert.showAndWait();
            });
        }
    }

}
