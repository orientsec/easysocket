package com.orientsec

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import java.nio.ByteOrder

class HeadHandler : LengthFieldBasedFrameDecoder {
    constructor(maxFrameLength: Int, lengthFieldOffset: Int, lengthFieldLength: Int) : super(
        maxFrameLength,
        lengthFieldOffset,
        lengthFieldLength
    )

    constructor(
        maxFrameLength: Int,
        lengthFieldOffset: Int,
        lengthFieldLength: Int,
        lengthAdjustment: Int,
        initialBytesToStrip: Int
    ) : super(
        maxFrameLength,
        lengthFieldOffset,
        lengthFieldLength,
        lengthAdjustment,
        initialBytesToStrip
    )

    constructor(
        maxFrameLength: Int,
        lengthFieldOffset: Int,
        lengthFieldLength: Int,
        lengthAdjustment: Int,
        initialBytesToStrip: Int,
        failFast: Boolean
    ) : super(
        maxFrameLength,
        lengthFieldOffset,
        lengthFieldLength,
        lengthAdjustment,
        initialBytesToStrip,
        failFast
    )

    constructor(
        byteOrder: ByteOrder,
        maxFrameLength: Int,
        lengthFieldOffset: Int,
        lengthFieldLength: Int,
        lengthAdjustment: Int,
        initialBytesToStrip: Int,
        failFast: Boolean
    ) : super(
        byteOrder,
        maxFrameLength,
        lengthFieldOffset,
        lengthFieldLength,
        lengthAdjustment,
        initialBytesToStrip,
        failFast
    )

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
