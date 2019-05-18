package com.geekbrains.april.cloud.box.common;

public class InfoMessage implements AbstractMessage {
    public enum MessageCode {
        AUTHORIZATION_FAILED,
        AUTHORIZATION_SUCCESSFUL,
        FILE_CORRUPTED
    }

    private String info1, info2;

    private MessageCode code;

    public InfoMessage(MessageCode code, String info1, String info2) {
        this.code = code;
        this.info1 = info1;
        this.info2 = info2;
    }

    public String getInfo1() {
        return info1;
    }

    public String getInfo2() {
        return info2;
    }

    public MessageCode getCode() {
        return code;
    }
}
