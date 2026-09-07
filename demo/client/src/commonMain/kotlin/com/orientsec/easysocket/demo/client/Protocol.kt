package com.orientsec.easysocket.demo.client

import com.orientsec.easysocket.HeadParser
import com.orientsec.easysocket.Packet
import com.orientsec.easysocket.PacketType
import com.orientsec.easysocket.request.Request
import com.orientsec.easysocket.session.SessionInitializer
import com.orientsec.easysocket.task.TaskBuilder
import com.orientsec.easysocket.task.send
import kotlinx.io.*

class MyHead(packetSize: Int, val taskId: Int, val packetType: PacketType) :
    HeadParser.Head(packetSize)

class MyHeadParser : HeadParser {
    override fun headSize(): Int {
        return 12
    }

    override fun parseHead(bytes: ByteArray): MyHead {
        val buffer = Buffer()
        buffer.write(bytes)
        val bodyLen = buffer.readInt()
        val taskId = buffer.readInt()
        val cmd = buffer.readInt()
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
        val authRequest = SimpleRequest("test", client.session, cmd = 1)
        return try {
            val sessionId = taskBuilder.buildTask(authRequest).send().toIntOrNull()
            if (sessionId != null) {
                client.session.sessionId = sessionId
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Invalid session id"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

class Session {
    var sessionId: Int = 0
}


class SimpleRequest(
    private val param: String,
    private val session: Session,
    private val cmd: Int = 2
) : Request<String>() {

    override fun encode(sequenceId: Int): ByteArray {
        val body = param.encodeToByteArray()
        val buffer = Buffer()
        buffer.writeInt(body.size)
        buffer.writeInt(sequenceId)
        //packet type
        buffer.writeInt(cmd)
        buffer.writeInt(session.sessionId)
        buffer.write(body)

        return buffer.readByteArray()
    }

    override fun decode(packet: Packet): String {
        return (packet.body as ByteArray).decodeToString()
    }
}
