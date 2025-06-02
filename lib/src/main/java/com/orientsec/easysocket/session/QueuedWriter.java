package com.orientsec.easysocket.session;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.EasyExecutor;
import com.orientsec.easysocket.client.BaseSocketClient;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.task.OperableTask;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

/**
 * A writer implementation that manages a queue of tasks to be written to a socket.
 * This class ensures that tasks are written sequentially and handles errors during the writing
 * process.
 */
class QueuedWriter implements Writer {
    // The session associated with this writer
    private final OperableSession session;
    // The socket used for writing data
    private final Socket socket;
    // Executor for handling write operations
    private final Executor writeExecutor;
    // Runner for scheduling tasks on the main thread
    private final EasyExecutor mainExecutor;
    // Queue for storing tasks to be written
    private final Deque<OperableTask<?>> writingQueue = new ArrayDeque<>();
    // Flag indicating whether a write operation is currently in progress
    private boolean isWriting = false;

    /**
     * Constructs a QueuedWriter instance with the specified parameters.
     *
     * @param session The session associated with this writer.
     * @param socket  The socket used for writing data.
     * @param client  The BaseSocketClient instance providing options and executors.
     */
    public QueuedWriter(OperableSession session, Socket socket, BaseSocketClient client) {
        this.session = session;
        this.socket = socket;
        this.writeExecutor = client.getOptions().getWriteExecutor();
        this.mainExecutor = client.getMainExecutor();
    }

    /**
     * Submits a task to the writing queue.
     *
     * @param task The task to be written.
     */
    @Override
    public void submit(@NonNull OperableTask<?> task) {
        enqueue(task);
    }

    /**
     * Adds a task to the writing queue and schedules the next write operation if none is in
     * progress.
     *
     * @param task The task to be added to the queue.
     */
    private void enqueue(@NonNull OperableTask<?> task) {
        writingQueue.add(task);
        if (!isWriting) {
            isWriting = true;
            scheduleNextWrite();
        }
    }

    /**
     * Schedules the next write operation from the queue.
     * If the queue is empty, marks the writer as not writing.
     */
    private void scheduleNextWrite() {
        if (writingQueue.isEmpty()) {
            isWriting = false;
            return;
        }

        OperableTask<?> nextTask = writingQueue.removeFirst();
        writeExecutor.execute(() -> write(nextTask));
    }

    /**
     * Writes data to the socket in the write thread.
     *
     * @param task The task containing the data to be written.
     */
    private void write(OperableTask<?> task) {
        mainExecutor.execute(task::onSendStart);
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(task.getData());
            outputStream.flush();
            mainExecutor.execute(task::onSendSuccess);
        } catch (IOException e) {
            session.getLogger().e("socket write error", e);
            EasyException ex = EasyException.create(ErrorCode.WRITE_ERROR, ErrorType.CONNECT,
                    "socket write aborted", session.getSuffix(), e);
            mainExecutor.execute(() -> {
                task.onSendFailure(ex);
                session.close(ex);
            });
        } finally {
            // Schedules the next write operation on the main thread
            mainExecutor.execute(this::scheduleNextWrite);
        }
    }

    /**
     * Cancels all tasks in the writing queue.
     */
    public void cancelAll() {
        writingQueue.clear();
    }

    /**
     * Cancels a specific task in the writing queue.
     *
     * @param task The task to be canceled.
     */
    @Override
    public void cancel(@NonNull OperableTask<?> task) {
        boolean removeFromWritingQueue = writingQueue.remove(task);
        session.getLogger().i("removed task " + task.getTaskId() +
                " from writing queue:" + removeFromWritingQueue);
    }
}