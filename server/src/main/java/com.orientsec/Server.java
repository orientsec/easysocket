package com.orientsec;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class Server {
    public void start(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializerImpl<>())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            System.out.println("ServerBootstrap error");
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    /**
     * @param <T>
     */
    public class ChannelInitializerImpl<T extends Channel> extends ChannelInitializer<T> {

        @Override
        protected void initChannel(T channel) throws Exception {
            channel.pipeline().addLast(new LengthFieldBasedFrameDecoder(200 * 1024, 0, 4, 12, 4));
            channel.pipeline().addLast(new ConnectHandler());
            channel.pipeline().addLast(new ServerHandler());
        }

    }

    public static void main(String[] args) {
        new Server().start(10010);
    }
}
