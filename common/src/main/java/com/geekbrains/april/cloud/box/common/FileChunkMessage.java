package com.geekbrains.april.cloud.box.common;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileChunkMessage implements AbstractMessage {
    private FileInfo fileInfo;
    private byte[] data;

    public FileInfo getFileInfo() throws CloneNotSupportedException {
        return (FileInfo) fileInfo.clone();
    }

    public byte[] getData() {
        return data;
    }

    public FileChunkMessage(Path path, FileInfo fileInfo) throws IOException {
        this.fileInfo = fileInfo;
        data = Files.readAllBytes(path);
    }

    public FileChunkMessage(byte[] data, FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        this.data = data;
    }
}
