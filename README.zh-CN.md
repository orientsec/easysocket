# EasySocket

[English](README.md)

面向 **Android**、**iOS** 与 **JVM** 的 Kotlin Multiplatform Socket 库：传输层可插拔
（Ktor / java.net.Socket / Network.framework），内置看门狗式心跳、自动重连与地址容灾、
基于协程的类型化请求/响应任务系统。

## 特性

- **Kotlin Multiplatform** —— 协议与生命周期层仅有一份实现，三种传输引擎：
  - `library/core` —— 引擎 + 基于 **Ktor**（`ktor-network`）的传输
  - `library/platform` —— **原生**传输：Android/JVM 用 `java.net.Socket`，
    iOS 用 `Network.framework`
- **看门狗心跳** —— 收到数据即“喂狗”，链路静默超过阈值才开始探测；探测失败以短间隔重试，
  超过次数判定连接死亡
- **自动重连** —— `ReconnectPolicy`（NONE / ACTIVE / ALWAYS）、地址列表容灾切换、
  网络恢复触发、后台存活时长感知
- **请求/响应任务** —— 类型化的 `Request<T>` / `Callback<T>`，带超时、重试与 taskId 关联
- **服务端推送** —— `PushManager` 处理服务端主动下发的消息
- **会话初始化** —— 连接建立后的登录/握手挂钩点
- **连接质量指标** —— 逐次记录 DNS / TCP / TLS 各阶段耗时
- **流量标记** —— 对 Socket 打标签，接入平台流量统计
- **单线程内核** —— 全部会话状态运行在单一协程调度器上，无锁、无跨线程可见性问题

## 项目结构

```
easysocket/
├── library/                  # 可发布产物
│   ├── core/                 # :library:core    —— 引擎 + Ktor 传输
│   └── platform/             # :library:platform —— 原生传输
└── demo/                     # 演示工程（不发布）
    ├── client/               # :demo:client      —— Compose Multiplatform 演示
    ├── androidDemo/          # :demo:androidDemo —— Android 壳（manifest + 资源）
    ├── iosDemo/              # 消费 ComposeApp framework 的 Xcode 工程
    └── server/               # Netty echo/auth 测试服务器（./gradlew :demo:server:run）
```

## 引入依赖

发布坐标：

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    // 引擎 + Ktor 传输（必需）
    implementation("com.orientsec.easysocket:core:1.0.0")

    // 可选：原生传输（java.net.Socket / Network.framework）
    implementation("com.orientsec.easysocket:platform:1.0.0")
}
```

> JitPack 上 group 会被重写为 `com.github.<user>.easysocket`。

环境要求：Kotlin 2.4+、minSdk 24、kotlinx-coroutines 1.11+。

## 快速开始

### 1. 初始化（进程内一次）

```kotlin
// Android —— 在 Application.onCreate() 中
EasySocket.initialize(NetworkObserver(applicationContext), AppLifecycleObserver())

// iOS —— 创建任何客户端之前
EasySocket.initialize(NetworkObserver(), AppLifecycleObserver())
```

### 2. 定义协议

实现 `HeadParser` 描述你的二进制帧格式：

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

### 3. 打开客户端

```kotlin
val client = Options.build {
    name = "my-socket"
    isDebuggable = true
    minLogLevel = Platform.LogLevel.DEBUG

    addressList = listOf(Address("192.168.1.10", 10010))

    // 传输引擎 —— 二选一：
    useSocketDefaults()   // platform 模块：java.net.Socket / Network.framework
    // useKtorDefaults() // core 模块：Ktor

    headParserProvider = { MyHeadParser() }
    sessionInitializerProvider = { MySessionInitializer(it) }  // 连接后登录

    requestTimeoutMills = 10_000
    connectTimeoutMills = 5_000
    reconnectPolicy = ReconnectPolicy.ACTIVE
    pulseDelaySeconds = 30        // 静默 30 秒后开始心跳
}.open()

client.start()
```

### 4. 发送请求

```kotlin
class EchoRequest(private val text: String) : Request<String>() {
    override fun encode(sequenceId: Int): ByteArray { /* ... */ }
    override fun decode(packet: Packet): String = packet.body.decodeToString()
}

// 方式一：suspend（挂起等待结果）
val res: String = client.buildTask(EchoRequest("hello")).send()

// 方式二：callback（回调在主线程；可细粒度实现 LifecycleCallback 观察各阶段）
client.buildTask(EchoRequest("hello"), object : DefaultCallback<String>() {
    override fun onSuccess(res: String) { /* 主线程 */ }
    override fun onFailure(t: Throwable) { /* ... */ }
}).execute()
```

### 5. 监听连接事件

```kotlin
client.addConnectionListener(object : ConnectionListener {
    override fun onConnecting(session: Session) {}
    override fun onConnected(session: Session) {}
    override fun onConnectFailed(session: Session, e: EasyException) {}
    override fun onAvailable(session: Session) {}                  // 登录完成
    override fun onDisconnected(session: Session, e: EasyException) {}
})
```

### 6. TLS

在 `Address` 上设置 `isSsl = true`。Android/JVM 原生传输需要注入 SSL 工厂：

```kotlin
sessionFactory = SocketSessionFactory(SSLSocketFactory.getDefault())
```

## 演示与测试服务器

demo 连接本地 Netty 服务器：

```bash
./gradlew :demo:server:run          # 监听 0.0.0.0:10010
```

然后运行 `demo/androidDemo`（Android）或 `demo/iosDemo` 的 Xcode scheme（iOS）。

## 更多文档

- [架构设计](docs/ARCHITECTURE.md) —— 分层、线程模型、心跳与重连设计

## 许可证

Apache License 2.0 —— 见 [LICENSE](LICENSE)。
