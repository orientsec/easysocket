package com.orientsec.easysocket.session;

import com.orientsec.easysocket.utils.Logger;

/**
 * An abstract class that provides a looping mechanism for executing tasks in a separate thread.
 * This class implements the `Runnable` interface and manages the lifecycle of the loop.
 */
public abstract class Looper implements Runnable {
    // The thread in which the loop runs
    private Thread thread;

    // A flag indicating whether the loop should stop
    private volatile boolean stop;

    // The number of times the loop has executed
    private long loopTimes = 0;

    // The name of the thread
    private String name;

    // Logger instance for logging messages
    private final Logger logger;

    // The error that caused the loop to stop, if any
    protected Throwable error;

    /**
     * Constructs a Looper instance with the specified logger.
     *
     * @param logger The logger used for logging messages.
     */
    protected Looper(Logger logger) {
        this.logger = logger;
    }

    /**
     * Starts the loop in a new thread.
     * If the loop is already running, this method does nothing.
     */
    public synchronized void start() {
        if (!stop) {
            String name = getClass().getSimpleName();
            this.name = name;
            thread = new Thread(this, name);
            loopTimes = 0;
            thread.start();
            logger.d(name + " is starting");
        }
    }

    /**
     * The main loop logic executed in the thread.
     * This method calls `beforeLoop`, executes the loop logic repeatedly,
     * and finally calls `loopFinish` when the loop stops.
     */
    @Override
    public final void run() {
        try {
            beforeLoop();
            while (!stop) {
                this.runInLoopThread();
                loopTimes++;
            }
        } catch (Throwable t) {
            error = t;
            logger.w(name + " is shutting down by error ", t);
        } finally {
            loopFinish();
        }
    }

    /**
     * Returns the number of times the loop has executed.
     *
     * @return The number of loop executions.
     */
    public long getLoopTimes() {
        return loopTimes;
    }

    /**
     * Called before the loop starts.
     * Subclasses must implement this method to perform any setup required before the loop begins.
     *
     * @throws Exception If an error occurs during setup.
     */
    protected abstract void beforeLoop() throws Exception;

    /**
     * The logic to be executed in each iteration of the loop.
     * Subclasses must implement this method to define the loop's behavior.
     *
     * @throws Exception If an error occurs during execution.
     */
    protected abstract void runInLoopThread() throws Exception;

    /**
     * Called when the loop finishes.
     * Subclasses must implement this method to perform any cleanup required after the loop ends.
     */
    protected abstract void loopFinish();

    /**
     * Stops the loop and interrupts the thread.
     * If the loop is not running, this method does nothing.
     */
    public synchronized void shutdown() {
        if (thread != null && !stop) {
            stop = true;
            thread.interrupt();
            thread = null;
        }
    }

    /**
     * Checks whether the loop is currently running.
     *
     * @return `true` if the loop is running, `false` otherwise.
     */
    public boolean isRunning() {
        return !stop;
    }
}