package com.geekbrains.april.cloud.box.server;

import com.geekbrains.april.cloud.box.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class MainHandler extends ChannelInboundHandlerAdapter {
    int user_id = 0;
    int chunksize = 0;
    String root_dir = "";

    private byte[] buffer;

    private Session session = new Session() {

        @Override
        public FileInfo getSavedFileInfo(FileInfo fileInfo) throws CloneNotSupportedException {
            return SQLHandler.getFileInfoDB(fileInfo, user_id);
        }

        @Override
        public void createAndSaveFileInfo(FileInfo fileInfo) throws IOException {
            fileInfo.position = 0;
            SQLHandler.insertOrUpdateWorkingFile(fileInfo, user_id);
        }

        @Override
        public String getFullFileName(FileInfo fileInfo) {
            return root_dir + "/" + fileInfo.MD5;
        }

        @Override
        public String getFullCodedFileName(FileInfo fileInfo) {
            return getFullFileName(fileInfo);
        }

        @Override
        public void updateIndiviualSend(FileInfo info) throws CloneNotSupportedException {
//TODO
        }

        @Override
        public void updateListSend() {
//TODO
        }

        @Override
        public void updateIndiviualReceive(FileInfo info) {
            SQLHandler.insertOrUpdateWorkingFile(info, user_id);
        }

        @Override
        public AbstractMessage updateListReceive() {
            return new FileListMessage(SQLHandler.getUserFilesList(user_id));
        }
    };

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof AuthMessage) {
            Auth(ctx, (AuthMessage) msg);
            ReferenceCountUtil.release(msg);
            return;
        }

        if (msg instanceof FileUploadToggleRequest) {
            AbstractMessage result = session.StartStopToggle(((FileUploadToggleRequest) msg).getFileInfo());
            if (result != null) ctx.writeAndFlush(result);
            ReferenceCountUtil.release(msg);
            return;
        }

        if (msg instanceof FileChunkMessage) {
            AbstractMessage result = session.Receive(((FileChunkMessage) msg).getFileInfo(), ((FileChunkMessage) msg).getData());
            if (result != null) ctx.writeAndFlush(result);
            ReferenceCountUtil.release(msg);
            return;
        }

        if (msg instanceof FileDownloadRequest) {
            AbstractMessage result = session.Send(((FileDownloadRequest) msg).getFileInfo(), buffer);
            if (result != null) ctx.writeAndFlush(result);
            ReferenceCountUtil.release(msg);
            return;
        }

        if (msg instanceof FileListRequest) {
            FileList(ctx);
            ReferenceCountUtil.release(msg);
            return;
        }

        if (msg instanceof FileDeleteRequest) {
            Delete(ctx, (FileDeleteRequest) msg);
            ReferenceCountUtil.release(msg);
            return;
        }

        if (msg == null) {
            return;
        }

        ReferenceCountUtil.release(msg);
    }


    private void FileList(ChannelHandlerContext ctx) {
        FileListMessage fileListMessage = new FileListMessage(SQLHandler.getUserFilesList(user_id));
        ctx.writeAndFlush(fileListMessage);
    }

    private void Delete(ChannelHandlerContext ctx, FileDeleteRequest msg) throws IOException {
        Path path = Paths.get(root_dir + "/" + msg.getFileInfo().MD5);
        if (Files.exists(path)) {
            if (SQLHandler.deleteWorkingFile(msg.getFileInfo(), user_id)) {
                Files.delete(path);
            }
        }
        FileList(ctx);
    }

    private void Auth(ChannelHandlerContext ctx, AuthMessage msg) {
        user_id = ctx.channel().attr(CloudBoxServer.user_id).get();
        chunksize = ctx.channel().attr(CloudBoxServer.chunksize).get();
        root_dir = ctx.channel().attr(CloudBoxServer.root_dir).get();
        buffer = new byte[chunksize];
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
