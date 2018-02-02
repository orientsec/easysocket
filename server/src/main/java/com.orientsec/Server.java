package com.orientsec;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Product: EasySocket
 * Package: com.orientsec
 * Time: 2018/01/25 09:32
 * Author: Fredric
 * coding is art not science
 */
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
            channel.pipeline().addLast(new LengthFieldBasedFrameDecoder(200 * 1024, 0, 4, 8, 4));
            channel.pipeline().addLast(new ServerHandler());
        }

    }

    public static void main(String[] args) {
        new Server().start(10010);
    }
}
