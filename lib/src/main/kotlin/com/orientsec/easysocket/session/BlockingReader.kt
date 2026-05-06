package com.orientsec.easysocket.session

import android.net.TrafficStats
import com.orientsec.easysocket.HeadParser
import com.orientsec.easysocket.Options
import com.orientsec.easysocket.client.BaseSocketClient
import com.orientsec.easysocket.error.EasyException
import com.orientsec.easysocket.error.ErrorCode
import com.orientsec.easysocket.error.ErrorType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.Socket

/**
 * A class responsible for reading data from a socket in a blocking manner.
 * This class extends the `Looper` class and implements the `Reader` interface.
 */
class BlockingReader(
    private val session: OperableSession,
    private val socket: Socket,
    client: BaseSocketClient
) : Looper(session.logger), Reader {

    // Input stream for reading data from the socket
    private lateinit var inputStream: InputStream

    // Parser for handling the packet headers
    private val headParser: HeadParser = client.headParser

    // Maximum size of the packet to read, in bytes
    private val maxReadSize: Long = client.options.maxReadSizeKb * 1024L

    // Configuration options for the reader
    private val options: Options = client.options

    private val scope = client.scope

    /**
     * Reads data from the input stream, parses the packet, and handles it.
     */
    override suspend fun read() {
        val headLength = headParser.headSize()
        val headBytes = ByteArray(headLength)
        readInputStream(inputStream, headBytes)
        val head = headParser.parseHead(headBytes)
        val packetSize = head.packetSize
        if (packetSize > maxReadSize) {
            throw Exception(
                "packet size: $packetSize is large than max size: $maxReadSize"
            )
        } else if (packetSize >= 0) {
            val data = ByteArray(packetSize)
            readInputStream(inputStream, data)
            val packet = headParser.decodePacket(head, data)
            session.handlePacket(packet)
        } else {
            throw Exception("negative packet size: $packetSize")
        }
    }

    /**
     * Reads data from the input stream into the provided byte array.
     */
    private suspend fun readInputStream(inputStream: InputStream, data: ByteArray) {
        withContext(Dispatchers.IO) {
            var readCount = 0
            val count = data.size
            while (readCount < count) {
                val len = inputStream.read(data, readCount, count - readCount)
                if (len == -1) {
                    throw IOException("input stream closed")
                }
                readCount += len
            }
        }
    }

    override fun beforeLoop() {
        TrafficStats.setThreadStatsTag(options.readStatsTag)
        inputStream = socket.getInputStream()
    }

    override suspend fun runInLoopThread() {
        read()
    }

    @Synchronized
    override fun loopFinish() {
        TrafficStats.clearThreadStatsTag()
        val e = EasyException(
            ErrorCode.READ_EXIT, ErrorType.CONNECT,
            "socket read aborted", session.suffix, error
        )
        if (isRunning()) {
            scope.launch { session.close(e) }
        }
    }
}
