package com.geekbrains.april.cloud.box.common;

public class FileDeleteRequest implements AbstractMessage {
    private FileInfo fileInfo;

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public FileDeleteRequest(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
}
