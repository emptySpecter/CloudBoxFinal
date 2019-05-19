package com.geekbrains.april.cloud.box.client;

import com.geekbrains.april.cloud.box.common.FileInfo;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.util.Callback;

public class FXFileInfo {

    FileInfo fileInfo = new FileInfo();
    boolean inProgress = false;
    private FloatProperty percent;

    public FXFileInfo(FileInfo info) {
        fileInfo.fileName = info.fileName;
        fileInfo.fileLength = info.fileLength;
        fileInfo.position = info.position;
        fileInfo.MD5 = info.MD5;
        percent = new SimpleFloatProperty(calculatePercent());
    }

    FileInfo getFileInfo() {
        try {
            return (FileInfo) fileInfo.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

// properties for FX TableView columns

    public String getFXFileName() {
        String result = fileInfo.fileName;
        if (inProgress) result = result + " (in progress)";
        return result;
    }

    public Long getFileSize() {
        return fileInfo.fileLength;
    }

    public float getPercent() {
        return percent.get();
    }

    public FloatProperty percentProperty() {
        return percent;
    }

    private float calculatePercent() {
        float result = 1.0f;
        if (fileInfo.fileLength != 0) result = ((float) fileInfo.position) / ((float) fileInfo.fileLength);
        return result;
    }

    public void setPosition(long position) {
        fileInfo.position = position;
        this.percent.set((calculatePercent()));
    }

    public boolean getInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    public static String FileInfoToFileName(FileInfo fileInfo) {
        return "$" + fileInfo.MD5 + "$" + fileInfo.fileLength + "$" + fileInfo.fileName;
    }

    public static FileInfo parseToFileInfo(String fileName, long position) {
        long size = position;
        int stringLength = fileName.length();
        String MD5 = "";
        if (stringLength > 34) {
            if (fileName.charAt(0) == '$' && fileName.charAt(33) == '$') {
                MD5 = fileName.substring(1, 33);
                if (MD5.matches("[0-9a-f]{32}")) {
                    int j = fileName.indexOf('$', 34);
                    String len = fileName.substring(34, j);
                    if (len.matches("^[1-9][0-9]*$")) {
                        size = Integer.parseInt(len);
                        fileName = fileName.substring(j + 1);
                    }
                }
            }
        }
        FileInfo fileInfo = new FileInfo();
        fileInfo.fileName = fileName;
        fileInfo.position = position;
        fileInfo.fileLength = size;
        if (fileName.length() == stringLength) MD5 = "";
        fileInfo.MD5 = MD5;
        return fileInfo;
    }


    public static class FileNameColumnMarkAsProcessing<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {
        @Override
        @SuppressWarnings("unchecked")
        public TableCell<S, T> call(TableColumn<S, T> p) {
            TableCell<S, T> cell = new TableCell<S, T>() {

                @Override
                public void updateItem(Object item, boolean empty) {
                    if (item == getItem()) {
                        return;
                    }
                    super.updateItem((T) item, empty);
                    if (item == null) {
                        super.setText(null);
                        super.setGraphic(null);
                    } else {
                        super.setText(item.toString());
                        super.setGraphic(null);
                        TableRow<FXFileInfo> currentRow = getTableRow();
                        FXFileInfo fi = currentRow.getItem();
                        if (fi == null) fi = currentRow.getTableView().getItems().get(currentRow.getIndex());
                        if (fi.getInProgress()) super.setStyle("-fx-text-fill: red;");
                        else super.setStyle("-fx-text-fill: black;");
                    }
                }
            };

            return cell;
        }

    }
}
