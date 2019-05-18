package com.geekbrains.april.cloud.box.common;

import java.io.Serializable;

public class FileInfo implements Serializable, Cloneable {
    public String fileName;
    public String MD5;
    public long fileLength;
    public long position;

    public FileInfo() {
        fileName = "";
        MD5 = "";
        fileLength = 0;
        position = 0;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
