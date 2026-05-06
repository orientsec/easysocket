package com.orientsec

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class Server {
    fun start(port: Int) {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()

        try {
            val serverBootstrap = ServerBootstrap()
            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(ChannelInitializerImpl<Channel>())
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val channelFuture = serverBootstrap.bind(port).sync()
            channelFuture.channel().closeFuture().sync()
        } catch (e: Exception) {
            println("ServerBootstrap error")
            e.printStackTrace()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    inner class ChannelInitializerImpl<T : Channel> : ChannelInitializer<T>() {
        override fun initChannel(channel: T) {
            channel.pipeline().addLast(LengthFieldBasedFrameDecoder(200 * 1024, 0, 4, 12, 4))
            channel.pipeline().addLast(ConnectHandler())
            channel.pipeline().addLast(ServerHandler())
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Server().start(10010)
        }
    }
}
