package com.geekbrains.april.cloud.box.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public abstract class Session {
    public HashMap<String, FileHelper.ChunksReceiver> chunksReceiverHashMap = new HashMap<>();
    public HashMap<String, FileHelper.ChunksSender> chunksSenderHashMap = new HashMap<>();

    abstract public FileInfo getSavedFileInfo(FileInfo fileInfo) throws CloneNotSupportedException;

    abstract public void createAndSaveFileInfo(FileInfo fileInfo) throws IOException;

    abstract public String getFullFileName(FileInfo fileInfo);

    abstract public String getFullCodedFileName(FileInfo fileInfo);

    abstract public void updateIndiviualSend(FileInfo info) throws CloneNotSupportedException;

    abstract public void updateListSend();

    abstract public void updateIndiviualReceive(FileInfo info) throws CloneNotSupportedException;

    abstract public AbstractMessage updateListReceive();

    public AbstractMessage StartStopToggle(FileInfo fileInfo) {
        try {
            FileHelper.ChunksReceiver receiver = chunksReceiverHashMap.get(fileInfo.MD5);

            if (receiver == null) {
                FileInfo savedFileInfo = getSavedFileInfo(fileInfo);
                if (savedFileInfo == null) {
                    createAndSaveFileInfo(fileInfo);
                    fileInfo.position = 0;
                } else {
                    if (!fileInfo.MD5.equals(savedFileInfo.MD5)) {
                        return new InfoMessage(InfoMessage.MessageCode.FILE_CORRUPTED, savedFileInfo.MD5, fileInfo.fileName);
                    }
                }

                receiver = new FileHelper.ChunksReceiver(Paths.get(getFullCodedFileName(fileInfo)));
                chunksReceiverHashMap.put(fileInfo.MD5, receiver);
            } else {
                chunksReceiverHashMap.remove(fileInfo.MD5);
                receiver.close();
                fileInfo.position = fileInfo.fileLength;  // to close corresponding ChunksSender and pause loading process
            }
            updateListSend();
            return new FileDownloadRequest(fileInfo);
        } catch (IOException | CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AbstractMessage Send(FileInfo info, byte[] buffer) throws IOException, CloneNotSupportedException {
        FileChunkMessage fileChunkMessage;
        FileHelper.ChunksSender sender = chunksSenderHashMap.get(info.MD5);

        if (info.position != info.fileLength) {
            updateIndiviualSend((FileInfo) info.clone());
            if (sender == null) {
                sender = new FileHelper.ChunksSender(getFullFileName(info), info.position);
                chunksSenderHashMap.put(info.MD5, sender);
                updateListSend();
            }
            int bytes = sender.read(buffer, info.position);
            if (bytes != -1) {
                fileChunkMessage = new FileChunkMessage(buffer, info); // regular chunk
            } else {
                fileChunkMessage = new FileChunkMessage(new byte[0], info);  // to close corresponding ChunksReceiver and pause loading process
            }
//TODO: realize sending chunks that has size less than chunksize
        } else {
            fileChunkMessage = new FileChunkMessage(new byte[0], info);  // to close corresponding ChunksReceiver and pause loading process
        }

        if (fileChunkMessage.getData().length == 0) {
            if (sender != null) {
                sender.close();
                chunksSenderHashMap.remove(info.MD5);
            }
            updateListSend();
        }
        return fileChunkMessage;

    }


    public AbstractMessage Receive(FileInfo fileInfo, byte[] buffer) throws IOException, CloneNotSupportedException {
        int dataSize = buffer.length;
        FileHelper.ChunksReceiver receiver = chunksReceiverHashMap.get(fileInfo.MD5);

        if (dataSize > 0) {
            long remain = fileInfo.fileLength - fileInfo.position;
            int len = (remain < dataSize) ? (int) remain : dataSize;
            if (receiver != null) {
                fileInfo.position = receiver.append(fileInfo.position, buffer, len);
                updateIndiviualReceive((FileInfo) fileInfo.clone());
                return new FileDownloadRequest(fileInfo);
            }
        } else {
            Path path = Paths.get(getFullCodedFileName(fileInfo));
            long fileSize = Files.size(path);
            if (fileSize == fileInfo.position) {
                String MD5 = FileHelper.calculateMD5(path);
                if (!fileInfo.MD5.equals(MD5)) {
                    if (Files.exists(path)) Files.delete(path);
                    fileInfo.position = 0;
                    updateIndiviualReceive((FileInfo) fileInfo.clone());
                    return new InfoMessage(InfoMessage.MessageCode.FILE_CORRUPTED, MD5, fileInfo.fileName);
                }
            }
            fileInfo.position = fileSize;
            updateIndiviualReceive((FileInfo) fileInfo.clone());
            if (fileInfo.position == fileInfo.fileLength) {
                chunksReceiverHashMap.remove(fileInfo.MD5);
                if (receiver != null) receiver.close();
                if (!getFullFileName(fileInfo).equals(getFullCodedFileName(fileInfo))) {
                    Files.move(path, Paths.get(getFullFileName(fileInfo)), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return updateListReceive();
        }
        return null;
    }

}