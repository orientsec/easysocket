package com.orientsec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Product: EasySocket
 * Package: com.orientsec
 * Time: 2018/01/25 16:58
 * Author: Fredric
 * coding is art not science
 */
public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Map<ChannelHandlerContext, Integer> sessions = new HashMap<>();
    private Random random = new Random();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int id = msg.readInt();
        int cmd = msg.readInt();
        int sessionId = msg.readInt();
        if (cmd == 0) {
            System.out.println("receive heart beat");
            ByteBuf resBuf = Unpooled.buffer();
            resBuf.writeInt(0);
            resBuf.writeInt(id);
            resBuf.writeInt(cmd);
            ctx.writeAndFlush(resBuf);
        } else if (cmd == 1) {
            System.out.println("receive auth");
            ByteBuf resBuf = Unpooled.buffer();
            resBuf.writeInt(4);
            resBuf.writeInt(id);
            resBuf.writeInt(cmd);
            int randomId = random.nextInt(100);
            resBuf.writeInt(randomId);
            ctx.writeAndFlush(resBuf);
            sessions.put(ctx, randomId);
        } else {
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);
            String req = new String(data);
            int cachedId = sessions.get(ctx);
            System.out.println("receive request, id:" + id + ", msg:" + req + ", session id:" + sessionId);
            String res;
            if (sessionId > 0 && cachedId == sessionId) {
                res = "我是只会学你说话:" + req + id;
            } else {
                res = "请先认证";
            }
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
