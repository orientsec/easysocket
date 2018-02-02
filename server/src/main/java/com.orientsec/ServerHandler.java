package com.orientsec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Product: EasySocket
 * Package: com.orientsec
 * Time: 2018/01/25 16:58
 * Author: Fredric
 * coding is art not science
 */
public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int id = msg.readInt();
        int cmd = msg.readInt();
        if (cmd == 0) {
            System.out.println("receive heart beat");
            ByteBuf resBuf = Unpooled.buffer();
            resBuf.writeInt(0);
            resBuf.writeInt(id);
            resBuf.writeInt(cmd);
            ctx.writeAndFlush(resBuf);
        } else {
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);
            String req = new String(data);
            System.out.println("receive request, id:" + id + " msg:" + req);
            String res = "我是只会学你说话:" + req + id;
            ByteBuf resBuf = Unpooled.buffer();
            byte[] resData = res.getBytes();
            resBuf.writeInt(resData.length);
            resBuf.writeInt(id);
            resBuf.writeInt(cmd);
            resBuf.writeBytes(resData);
            ctx.writeAndFlush(resBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
