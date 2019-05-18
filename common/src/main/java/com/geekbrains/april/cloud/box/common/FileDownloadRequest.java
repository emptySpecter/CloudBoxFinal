package com.geekbrains.april.cloud.box.common;

public class FileDownloadRequest implements AbstractMessage {
    private FileInfo fileInfo;

    public FileDownloadRequest(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public FileInfo getFileInfo() throws CloneNotSupportedException {
        return (FileInfo) fileInfo.clone();
    }

}
