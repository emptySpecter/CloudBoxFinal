package com.geekbrains.april.cloud.box.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHelper {

    public static String calculateMD5(Path path) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try (InputStream is = Files.newInputStream(path);
             DigestInputStream dis = new DigestInputStream(is, md)) {
            byte[] buf = new byte[1024];
            while (dis.read(buf, 0, 1024) != -1) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] digest = md.digest();
        StringBuffer stringBuffer = new StringBuffer();
        for (byte bytes : digest) {
            stringBuffer.append(String.format("%02x", bytes & 0xff));
        }
        return stringBuffer.toString();
    }

    public static class ChunksSender {
        private FileInputStream in;
        FileChannel channel;

        public ChunksSender(String name, long position) throws IOException {
            in = new FileInputStream(name);
            channel = in.getChannel();
        }

        public int read(byte[] buffer, long position) throws IOException {
            channel.position(position);
            return in.read(buffer);
        }

        public void close() throws IOException {
            in.close();
        }

    }

    public static class ChunksReceiver {
        private OutputStream out;
        long currentSize;

        public ChunksReceiver(Path path) throws IOException {
            out = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            currentSize = Files.size(path);
        }

        public long append(long position, byte[] buffer, int len) throws IOException {
//TODO: waiting to be more smart realizated
            if (position == currentSize) {
                out.write(buffer, 0, len);
                currentSize += len;
            }
            return currentSize;
        }

        public void close() throws IOException {
            out.close();
        }

    }

}
