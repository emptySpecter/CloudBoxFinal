package com.geekbrains.april.cloud.box.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.AttributeKey;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CloudBoxServer {
    final static AttributeKey<Integer> user_id = AttributeKey.valueOf("user_id");
    final static AttributeKey<Integer> chunksize = AttributeKey.valueOf("chunksize");
    final static AttributeKey<String> root_dir = AttributeKey.valueOf("root_dir");

    public void run() throws Exception {
        Properties prop = new Properties();
        try (InputStream input = CloudBoxServer.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                throw new IOException();
            }
            //load a properties file from class path
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (!SQLHandler.connect(prop.getProperty("db.url"), prop.getProperty("db.user"), prop.getProperty("db.password"), prop.getProperty("db.driver"))) {
            throw new RuntimeException("Не удалось подключиться к БД");
        }
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthHandler(),
                                    new MainHandler()
                            );
                            socketChannel.attr(user_id).set(0);
                            socketChannel.attr(chunksize).set(Integer.parseInt(prop.getProperty("server.chunksize")));
                            socketChannel.attr(root_dir).set(prop.getProperty("server.root_dir"));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(Integer.parseInt(prop.getProperty("server.port"))).sync();
            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            SQLHandler.disconnect();
        }
    }

    public static void main(String[] args) throws Exception {
        new CloudBoxServer().run();

    }
}
