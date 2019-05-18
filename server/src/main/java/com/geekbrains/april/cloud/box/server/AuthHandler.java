package com.geekbrains.april.cloud.box.server;

import com.geekbrains.april.cloud.box.common.AuthMessage;
import com.geekbrains.april.cloud.box.common.InfoMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private Boolean isAuthorized = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        }
        if (msg instanceof AuthMessage) {
            AuthMessage auth = (AuthMessage) msg;
            String loginToCheck = auth.getLogin();
            String passwordToCheck = auth.getPassword();
            int user_id = SQLHandler.authorize(loginToCheck, passwordToCheck);
            this.isAuthorized = true;
            if (user_id != 0) {
                ctx.channel().attr(CloudBoxServer.user_id).set(user_id);
                ReferenceCountUtil.release(msg);
                ctx.writeAndFlush(new InfoMessage(InfoMessage.MessageCode.AUTHORIZATION_SUCCESSFUL, "", ""));
            } else {
                ctx.writeAndFlush(new InfoMessage(InfoMessage.MessageCode.AUTHORIZATION_FAILED, "", ""));
            }
        }
        ctx.fireChannelRead(msg);
    }
}
