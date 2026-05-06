package com.orientsec

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.Random

class ServerHandler : SimpleChannelInboundHandler<ByteBuf>() {
    private val sessions = mutableMapOf<ChannelHandlerContext, Int>()
    private val random = Random()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val id = msg.readInt()
        val cmd = msg.readInt()
        val sessionId = msg.readInt()
        when (cmd) {
            0 -> pulse(ctx, id, cmd)
            1 -> auth(ctx, msg, id, cmd)
            else -> onRequest(ctx, msg, id, cmd, sessionId)
        }
    }

    private fun pulse(ctx: ChannelHandlerContext, id: Int, cmd: Int) {
        println("receive heart beat")
        val resBuf = Unpooled.buffer()
        resBuf.writeInt(0)
        resBuf.writeInt(id)
        resBuf.writeInt(cmd)
        ctx.writeAndFlush(resBuf)
    }

    private fun auth(ctx: ChannelHandlerContext, msg: ByteBuf, id: Int, cmd: Int) {
        val data = ByteArray(msg.readableBytes())
        msg.readBytes(data)
        val req = String(data)
        println("receive auth user name: $req")

        val randomId = random.nextInt(100)
        write(ctx, randomId.toString(), id, cmd)
        sessions[ctx] = randomId
    }

    private fun onRequest(ctx: ChannelHandlerContext, msg: ByteBuf, id: Int, cmd: Int, sessionId: Int) {
        val data = ByteArray(msg.readableBytes())
        msg.readBytes(data)
        val req = String(data)
        val cachedId = sessions[ctx]
        println("receive request, id:$id, msg:$req, session id:$sessionId")
        val res = if (sessionId > 0 && cachedId == sessionId) {
            "我是只会学你说话:$req$id"
        } else {
            "请先认证"
        }
        write(ctx, res, id, cmd)
    }

    private fun write(ctx: ChannelHandlerContext, msg: String, id: Int, cmd: Int) {
        val resBuf = Unpooled.buffer()
        val resData = msg.toByteArray()
        resBuf.writeInt(resData.size)
        resBuf.writeInt(id)
        resBuf.writeInt(cmd)
        resBuf.writeBytes(resData)
        ctx.writeAndFlush(resBuf)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
