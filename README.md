# EasySocket

[中文文档](README.zh-CN.md)

A Kotlin Multiplatform socket library for **Android**, **iOS** and **JVM**, with a pluggable
transport layer (Ktor / java.net.Socket / Network.framework), watchdog-style heartbeat,
automatic reconnection with address failover, and a typed request/response task system built
on coroutines.

## Features

- **Kotlin Multiplatform** — one protocol & lifecycle layer, three transports:
  - `library/core` — engine + **Ktor** based transport (`ktor-network`)
  - `library/platform` — **native** transports: `java.net.Socket` on Android/JVM,
    `Network.framework` on iOS
- **Watchdog heartbeat** — feeds on inbound traffic, only probes after the line goes quiet;
  retries with a short interval, then declares the connection dead
- **Reconnection** — `ReconnectPolicy` (NONE / ACTIVE / ALWAYS), address-list failover,
  network-restore trigger, background-duration awareness
- **Request/Response tasks** — typed `Request<T>` / `Callback<T>` with timeout, retry and
  task-id correlation
- **Server push** — `PushManager` for messages initiated by the server
- **Session initialization** — hook for login/handshake after the connection is established
- **Connection quality metrics** — per-connection timing of DNS / TCP / TLS phases
- **Traffic profiling** — tag/untag sockets for platform traffic statistics
- **Single-threaded core** — all session state lives on one coroutine dispatcher; no locks,
  no cross-thread visibility issues

## Project layout

```
easysocket/
├── library/                  # publishable artifacts
│   ├── core/                 # :library:core   — engine + Ktor transport
│   └── platform/             # :library:platform — native transports
└── demo/                     # demo application (not published)
    ├── client/               # :demo:client      — Compose Multiplatform demo
    ├── androidDemo/          # :demo:androidDemo — Android shell (manifest + resources)
    ├── iosDemo/              # Xcode project consuming the ComposeApp framework
    └── server/               # Netty echo/auth test server (./gradlew :demo:server:run)
```

## Installation

Published coordinates:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    // Engine + Ktor transport (required)
    implementation("com.orientsec.easysocket:core:1.0.0")

    // Optional: native transports (java.net.Socket / Network.framework)
    implementation("com.orientsec.easysocket:platform:1.0.0")
}
```

> On JitPack the group is rewritten to `com.github.<user>.easysocket`.

Requirements: Kotlin 2.4+, minSdk 24, kotlinx-coroutines 1.11+.

## Quick start

### 1. Initialize (once per process)

```kotlin
// Android — in Application.onCreate()
EasySocket.initialize(NetworkObserver(applicationContext), AppLifecycleObserver())

// iOS — before creating any client
EasySocket.initialize(NetworkObserver(), AppLifecycleObserver())
```

### 2. Define your protocol

Implement `HeadParser` to describe your binary framing:

```kotlin
class MyHeadParser : HeadParser {
    override fun headSize(): Int = 12

    override fun parseHead(bytes: ByteArray): HeadParser.Head {
        val buffer = Buffer().write(bytes)
        val bodyLen = buffer.readInt()
        val taskId = buffer.readInt()
        val cmd = buffer.readInt()
        val packetType = if (cmd == 0) PacketType.PULSE else PacketType.RESPONSE
        return MyHead(bodyLen, taskId, packetType)
    }

    override fun decodePacket(head: HeadParser.Head, body: ByteArray): Packet =
        Packet((head as MyHead).packetType, head.taskId, body)
}
```

### 3. Open a client

```kotlin
val client = Options.build {
    name = "my-socket"
    isDebuggable = true
    minLogLevel = Platform.LogLevel.DEBUG

    addressList = listOf(Address("192.168.1.10", 10010))

    // transport — pick one:
    useSocketDefaults()   // platform module: java.net.Socket / Network.framework
    // useKtorDefaults() // core module: Ktor

    headParserProvider = { MyHeadParser() }
    sessionInitializerProvider = { MySessionInitializer(it) }  // login after connect

    requestTimeoutMills = 10_000
    connectTimeoutMills = 5_000
    reconnectPolicy = ReconnectPolicy.ACTIVE
    pulseDelaySeconds = 30        // heartbeat after 30s of silence
}.open()

client.start()
```

### 4. Send requests

```kotlin
class EchoRequest(private val text: String) : Request<String>() {
    override fun encode(sequenceId: Int): ByteArray { /* ... */ }
    override fun decode(packet: Packet): String = packet.body.decodeToString()
}

client.buildTask(EchoRequest("hello"), object : DefaultCallback<String>() {
    override fun onSuccess(res: String) { /* main thread */ }
    override fun onFailure(t: Throwable) { /* ... */ }
}).execute()
```

### 5. Observe the connection

```kotlin
client.addConnectionListener(object : ConnectionListener {
    override fun onConnecting(session: Session) {}
    override fun onConnected(session: Session) {}
    override fun onConnectFailed(session: Session, e: EasyException) {}
    override fun onAvailable(session: Session) {}                  // login done
    override fun onDisconnected(session: Session, e: EasyException) {}
})
```

### 6. TLS

Set `isSsl = true` on the `Address`. On Android/JVM with the native transport, inject an
SSL socket factory:

```kotlin
sessionFactory = SocketSessionFactory(SSLSocketFactory.getDefault())
```

## Demo & test server

The demo connects to a local Netty server:

```bash
./gradlew :demo:server:run          # listens on 0.0.0.0:10010
```

Then run `demo/androidDemo` (Android) or the `demo/iosDemo` Xcode scheme (iOS).

## Documentation

- [Architecture](docs/ARCHITECTURE.md) — layering, threading model, heartbeat & reconnect design

## License

Apache License 2.0 — see [LICENSE](LICENSE).
