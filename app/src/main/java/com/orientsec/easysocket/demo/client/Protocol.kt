package com.orientsec.easysocket.demo.client

import com.orientsec.easysocket.HeadParser
import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.PacketType
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.SessionInitializer
import com.orientsec.easysocket.task.Callback
import com.orientsec.easysocket.task.DefaultCallback
import com.orientsec.easysocket.task.TaskBuilder
import kotlinx.coroutines.CompletableDeferred
import java.nio.ByteBuffer

class MyHead(packetSize: Int, val taskId: Int, val packetType: PacketType) :
    HeadParser.Head(packetSize)

class MyHeadParser : HeadParser {
    override fun headSize(): Int {
        return 12
    }

    override fun parseHead(bytes: ByteArray): MyHead {
        val byteBuffer = ByteBuffer.wrap(bytes)
        val bodyLen = byteBuffer.getInt()
        val taskId = byteBuffer.getInt()
        val cmd = byteBuffer.getInt()
        val packetType = if (cmd == 0) {
            PacketType.PULSE
        } else {
            PacketType.RESPONSE
        }
        return MyHead(bodyLen, taskId, packetType)
    }

    override fun decodePacket(head: HeadParser.Head, bodyBytes: ByteArray): Packet {
        val myHead = head as MyHead
        return Packet(myHead.packetType, myHead.taskId, bodyBytes)
    }
}

class MySessionInitializer(private val client: Client) : SessionInitializer {
    override suspend fun start(taskBuilder: TaskBuilder): Result<Unit> {
        val authRequest = SimpleRequest("test", client.session)
        val deferred = CompletableDeferred<Result<Unit>>()
        val callback: Callback<String> = object : DefaultCallback<String>() {
            override fun onSuccess(res: String) {
                client.session.sessionId = res.toInt()
                deferred.complete(Result.success(Unit))
            }

            override fun onFailure(t: Throwable) {
                deferred.complete(Result.failure(t))
            }
        }
        taskBuilder.buildTask(authRequest, callback).execute()
        return deferred.await()
    }
}

class Session {
    var sessionId: Int = 0
}


class SimpleRequest @JvmOverloads constructor(
    private val param: String,
    private val session: Session,
    private val cmd: Int = 2
) : Request<String>() {

    override fun encode(sequenceId: Int): ByteArray {
        val body = param.toByteArray()
        val byteBuffer = ByteBuffer.allocate(16)
        byteBuffer.putInt(body.size)
        byteBuffer.putInt(sequenceId)
        //packet type
        byteBuffer.putInt(cmd)
        byteBuffer.putInt(session.sessionId)

        val head = byteBuffer.array()
        val sendBytes = ByteArray(head.size + body.size)
        System.arraycopy(head, 0, sendBytes, 0, head.size)
        System.arraycopy(body, 0, sendBytes, head.size, body.size)
        return sendBytes
    }

    override fun decode(packet: Packet): String {
        return String(packet.body as ByteArray)
    }
}
