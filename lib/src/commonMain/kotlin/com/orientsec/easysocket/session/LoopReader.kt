package com.orientsec.easysocket.session

import com.orientsec.easysocket.utils.Logger
import kotlinx.coroutines.*

/**
 * 循环读取的抽象基类，提供基于协程的循环读取机制。
 *
 * 在 [CoroutineScope] 中启动一个协程，持续执行 [read] 操作，
 * 直到协程被取消或发生异常。适用于需要持续从 Socket 读取数据的场景。
 *
 * 生命周期：
 * 1. [start] - 启动循环读取
 * 2. [beforeLoop] -> [read] (循环) -> [loopFinish] - 读取流程
 * 3. [shutdown] - 停止循环读取
 *
 * @param logger 日志记录器
 * @param scope 协程作用域
 */
abstract class LoopReader(private val logger: Logger, private val scope: CoroutineScope) : Reader {
    /** 循环读取的协程任务 */
    private var job: Job? = null

    /** 循环已执行的次数 */
    var loopTimes: Long = 0
        private set

    /** 导致循环停止的错误，如果正常停止则为 null */
    protected var error: Throwable? = null

    /**
     * 启动循环读取。
     * 如果循环已在运行，则不执行任何操作。
     */
    override fun start() {
        if (job == null || job?.isCompleted == true) {
            loopTimes = 0
            error = null
            job = scope.launch {
                runLoop()
            }
            logger.d("${this::class.simpleName} is starting")
        }
    }

    /**
     * 循环读取的主逻辑。
     * 依次执行 [beforeLoop]、循环 [read]，最后执行 [loopFinish]。
     */
    private suspend fun runLoop() {
        try {
            beforeLoop()
            while (currentCoroutineContext().isActive) {
                read()
                loopTimes++
            }
        } catch (_: CancellationException) {
            // 协程被取消，正常退出
            logger.d("${this::class.simpleName} was cancelled")
        } catch (t: Throwable) {
            // 发生异常，记录错误
            error = t
            logger.w("${this::class.simpleName} is shutting down by error ", t)
        } finally {
            // 确保循环结束回调一定被执行
            withContext(NonCancellable) {
                loopFinish()
            }
        }
    }

    /**
     * 循环开始前的初始化操作。
     * 子类可在此方法中执行初始化逻辑，如打开输入流等。
     *
     * @throws Exception 如果初始化过程中发生错误
     */
    @Throws(Exception::class)
    protected abstract fun beforeLoop()

    /**
     * 循环结束后的清理操作。
     * 无论是正常结束还是异常结束，此方法都会被调用。
     * 子类可在此方法中执行资源释放、错误通知等操作。
     */
    protected abstract fun loopFinish()

    /**
     * 停止循环读取，取消协程任务。
     */
    override fun shutdown() {
        job?.cancel()
        job = null
    }

    /**
     * 检查循环是否正在运行。
     *
     * @return true 如果循环正在运行
     */
    fun isRunning(): Boolean {
        return job?.isActive == true
    }
}