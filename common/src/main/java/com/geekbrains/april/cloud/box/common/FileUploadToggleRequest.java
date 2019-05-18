package com.geekbrains.april.cloud.box.common;

public class FileUploadToggleRequest implements AbstractMessage {
    private FileInfo fileInfo;

    public FileUploadToggleRequest(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

}
