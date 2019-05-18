package com.geekbrains.april.cloud.box.client;

import com.geekbrains.april.cloud.box.common.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;


public class MainController implements Initializable {

    @FXML
    TableView<FXFileInfo> localfilesTable;

    @FXML
    TableView<FXFileInfo> remotefilesTable;

    @FXML
    TableColumn<FXFileInfo, Number> tcLoadedRem;

    @FXML
    TableColumn<FXFileInfo, Number> tcLoadedLoc;

    @FXML
    Button btnDeleteRem, btnRefreshRem, btnDownloadRem;

    @FXML
    Button btnDeleteLoc, btnRefreshLoc, btnUploadLoc;

    @FXML
    Label titleRem;

    private String user = "";
    private String root_dir = "";


    private Predicate<String> senderInProgress;
    private Predicate<String> receiverInProgress;
    private Function<FileInfo, AbstractMessage> toggleClientReceiver;

    private HashMap<String,String> name2MD5 = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        localfilesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                String btnText = "Upload";
                if (newSelection.getPercent() < 0.999999 || UnderDownloaded(newSelection.fileInfo, remotefilesTable) != null) {
                    btnText = "Resume";
                    if (newSelection.inProgress) btnText = "Pause";
                }
                btnUploadLoc.setText(btnText);
            }
        });

        remotefilesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                String btnText = "Download";
                if (newSelection.getPercent() < 0.999999  || UnderDownloaded(newSelection.fileInfo, localfilesTable) != null) {
                    btnText = "Resume";
                    if (newSelection.inProgress) btnText = "Pause";
                }
                btnDownloadRem.setText(btnText);
            }
        });

        tcLoadedRem.setCellValueFactory(cellData -> cellData.getValue().percentProperty());
        tcLoadedLoc.setCellValueFactory(cellData -> cellData.getValue().percentProperty());

        Network.sendMsg(new FileListRequest());

    }

    public void setUser(String user) {
        this.user = user;
        titleRem.setText("Remote: " + user);

    }

    public void setRoot_dir(String root_dir) {
        this.root_dir = root_dir;
    }

    public void setCallbacks(Predicate<String> senderInProgress, Predicate<String> receiverInProgress, Function<FileInfo, AbstractMessage> toggleClientReceiver) {
        this.senderInProgress = senderInProgress;
        this.receiverInProgress = receiverInProgress;
        this.toggleClientReceiver = toggleClientReceiver;
        refreshLocalFilesList();
    }

    public void pressBtnDeleteRem(ActionEvent actionEvent) {
        FileInfo info = remotefilesTable.getSelectionModel().getSelectedItem().getFileInfo();
        Network.sendMsg(new FileDeleteRequest(info));
    }

    public void pressBtnRefreshRem(ActionEvent actionEvent) {
        Network.sendMsg(new FileListRequest());
    }

    public void pressBtnDownloadRem(ActionEvent actionEvent) {
        FileInfo info = remotefilesTable.getSelectionModel().getSelectedItem().getFileInfo();
        if (info.position < info.fileLength) {
            Network.sendMsg(new FileUploadToggleRequest(info));
            Network.sendMsg(new FileListRequest());
            refreshLocalFilesList();
        } else {
            AbstractMessage result = toggleClientReceiver.apply(info);
            if (result != null) {
                Network.sendMsg(result);
                Network.sendMsg(new FileListRequest());
            }
        }
    }

    public void pressBtnDeleteLoc(ActionEvent actionEvent) throws IOException {
        FileInfo info = localfilesTable.getSelectionModel().getSelectedItem().getFileInfo();
        String longFileName = root_dir + "/" + info.fileName;
        Files.delete(Paths.get(longFileName));
        refreshLocalFilesList();
    }

    public void pressBtnRefreshLoc(ActionEvent actionEvent) {
        refreshLocalFilesList();
    }

    public void pressBtnUploadLoc(ActionEvent actionEvent) {
        FileInfo info = localfilesTable.getSelectionModel().getSelectedItem().getFileInfo();
        if(info.position == info.fileLength) {
            FileInfo udFileInfo = UnderDownloaded(info, remotefilesTable);
            if (udFileInfo == null) {
                if (info.MD5.equals("")) info.MD5 = FileHelper.calculateMD5(Paths.get(root_dir + "/" + info.fileName));
                name2MD5.put(info.fileName, info.MD5);
                info.position = 0;
            } else {
                info.MD5 = udFileInfo.MD5;
            }
            Network.sendMsg(new FileUploadToggleRequest(info));
            Network.sendMsg(new FileListRequest());
        } else {

        }
    }

    private FileInfo UnderDownloaded(FileInfo info, TableView<FXFileInfo> tableView) {
        FileInfo fileInfo = null;
        try {
            FXFileInfo fxFileInfo = tableView.getItems().stream()
                    .filter(o -> o.getPercent() < 0.999999)
                    .filter(o -> o.fileInfo.MD5.equals(info.MD5))
                    .findFirst().get();
            fileInfo = fxFileInfo.getFileInfo();
        } catch (NoSuchElementException e) {
        }
        return fileInfo;
    }

    FXFileInfo getFileInfoFX(Path p) {
        try {
            String fileName = p.getFileName().toString();
            long position = Files.size(p);
            return new FXFileInfo(FXFileInfo.parseToFileInfo(fileName, position));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void refreshLocalFilesList() {
        localfilesTable.getItems().stream().forEach( o -> {if(!o.fileInfo.MD5.isEmpty()) name2MD5.put(o.fileInfo.fileName, o.fileInfo.MD5);});
        localfilesTable.getItems().clear();
        try {
            Files.list(Paths.get(root_dir))
                    .filter(p -> !Files.isDirectory(p))
                    .map(p -> getFileInfoFX(p))
                    .filter(o -> o != null)
                    .forEach(o -> {
                        String res = name2MD5.get(o.fileInfo.fileName);
                        if(res != null) o.fileInfo.MD5 = res;
                        o.setInProgress(senderInProgress.test(o.fileInfo.MD5) || receiverInProgress.test(o.fileInfo.MD5));
                        localfilesTable.getItems().add(o);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!localfilesTable.getItems().isEmpty()) localfilesTable.getSelectionModel().select(0);
    }

    public void refreshRemoteFilesList(ArrayList<FileInfo> list) {
        remotefilesTable.getItems().clear();
        for (FileInfo info : list) {
            FXFileInfo fxinfo = new FXFileInfo(info);
            fxinfo.setInProgress(senderInProgress.test(info.MD5) || receiverInProgress.test(info.MD5));
            remotefilesTable.getItems().add(fxinfo);
        }
        if (!remotefilesTable.getItems().isEmpty()) remotefilesTable.getSelectionModel().select(0);
    }

    public void updateRemoteFileInfo(FileInfo fileInfo) {
        remotefilesTable.getItems().stream()
                .filter(o -> o.getInProgress())
                .filter(o -> o.fileInfo.fileName.equals(fileInfo.fileName))
                .findFirst().ifPresent(o -> o.setPosition(fileInfo.position));
    }

    public void updateLocalFileInfo(FileInfo fileInfo) {
        localfilesTable.getItems().stream()
                .filter(o -> o.getInProgress())
                .filter(o -> o.fileInfo.fileName.equals(fileInfo.fileName))
                .findFirst().ifPresent(o -> o.setPosition(fileInfo.position));
    }
}

