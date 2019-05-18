package com.geekbrains.april.cloud.box.common;

import java.util.ArrayList;

public class FileListMessage implements AbstractMessage {
    ArrayList<FileInfo> list;

    public ArrayList<FileInfo> getList() {
        return list;
    }

    public FileListMessage(ArrayList<FileInfo> list) {
        this.list = list;
    }

}
