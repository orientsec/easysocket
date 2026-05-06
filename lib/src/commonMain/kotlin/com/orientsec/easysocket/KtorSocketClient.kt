package com.orientsec.easysocket

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.atomicfu.atomic
import kotlin.coroutines.CoroutineContext

class KtorSocketClient(
    override val options: Options,
    private val headParser: HeadParser
) : SocketClient {
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    override val connectionState = _connectionState.asSharedFlow()

    private var socket: Socket? = null
    private var readChannel: ByteReadChannel? = null
    private var writeChannel: ByteWriteChannel? = null
    private var job: Job? = null
    private val scope = CoroutineScope(options.connectionDispatcher + SupervisorJob())
    
    private val taskCounter = atomic(1)
    private val pendingRequests = mutableMapOf<Int, CompletableDeferred<Any>>()

    override suspend fun connect() {
        if (isConnected()) return

        _connectionState.emit(ConnectionState.Connecting)
        
        val address = options.addressList.first()
        
        try {
            val selectorManager = SelectorManager(options.connectionDispatcher)
            val socketBuilder = aSocket(selectorManager).tcp()
            
            val newSocket = withTimeout(options.connectTimeoutMillis) {
                socketBuilder.connect(address.host, address.port)
            }
            
            socket = newSocket
            readChannel = newSocket.openReadChannel()
            writeChannel = newSocket.openWriteChannel(autoFlush = true)
            
            _connectionState.emit(ConnectionState.Connected)
            
            startReading()
        } catch (e: Exception) {
            _connectionState.emit(ConnectionState.Disconnected(e))
            throw e
        }
    }

    private fun startReading() {
        job = scope.launch {
            try {
                val channel = readChannel ?: return@launch
                while (isActive) {
                    val headSize = headParser.headSize()
                    val headBuffer = ByteArray(headSize)
                    channel.readFully(headBuffer)
                    
                    val head = headParser.parseHead(headBuffer)
                    val bodySize = head.packetSize - headSize
                    val bodyBuffer = ByteArray(bodySize)
                    channel.readFully(bodyBuffer)
                    
                    val packet = headParser.decodePacket(head, bodyBuffer)
                    handlePacket(packet)
                }
            } catch (e: Exception) {
                if (isActive) {
                    disconnect()
                    _connectionState.emit(ConnectionState.Disconnected(e))
                }
            }
        }
    }

    private fun handlePacket(packet: Packet) {
        if (packet.type == PacketType.RESPONSE) {
            val deferred = pendingRequests.remove(packet.taskId)
            deferred?.complete(packet.body)
        } else if (packet.type == PacketType.PUSH) {
            // Handle push messages, maybe through another SharedFlow
        }
    }

    override suspend fun disconnect() {
        job?.cancel()
        socket?.close()
        socket = null
        readChannel = null
        writeChannel = null
        
        val error = Exception("Disconnected")
        pendingRequests.values.forEach { it.completeExceptionally(error) }
        pendingRequests.clear()
        
        _connectionState.emit(ConnectionState.Disconnected())
    }

    override fun isConnected(): Boolean {
        return socket != null && !socket!!.isClosed
    }

    override suspend fun send(data: ByteArray) {
        val channel = writeChannel ?: throw Exception("Not connected")
        channel.writeFully(data)
    }

    override suspend fun <T> request(request: Request<T>): T {
        if (!isConnected()) connect()
        
        val taskId = taskCounter.getAndIncrement()
        val deferred = CompletableDeferred<Any>()
        pendingRequests[taskId] = deferred
        
        try {
            val data = request.encode(taskId)
            send(data)
            
            val responseBody = withTimeout(options.requestTimeoutMillis) {
                deferred.await()
            }
            
            @Suppress("UNCHECKED_CAST")
            return responseBody as T
        } catch (e: Exception) {
            pendingRequests.remove(taskId)
            throw e
        }
    }
}
