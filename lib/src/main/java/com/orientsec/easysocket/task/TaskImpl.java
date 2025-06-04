package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.EasyExecutor;
import com.orientsec.easysocket.client.BaseSocketClient;
import com.orientsec.easysocket.error.EasyException;
import com.orientsec.easysocket.error.ErrorCode;
import com.orientsec.easysocket.error.ErrorType;
import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.Request;
import com.orientsec.easysocket.session.OperableSession;
import com.orientsec.easysocket.session.Writer;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a request task.
 *
 * <p><b>Lifecycle overview:</b></p>
 * A Task goes through the following ordered lifecycle states:
 *
 * <pre>
 * +-----------+        +--------+        +--------+       +--------+        +----------+
 * |   Start   | -----> | Encode | -----> | Submit |       | Decode | -----> | Complete |
 * +-----------+        +--------+        +--------+       +--------+        +----------+
 *      |                    +                 |                +
 *      |                    |                 |                |
 *      |                    |                 |                |
 *      |                    |                 |           +----------------+
 * (no available session)    |                 |           |onPacketReceived|  Receive response
 *      |                    |                 v           +----------------+
 *      v                    |                 |
 *      v                    |             +--------+         +-------+
 *   +-------+               |             |Enqueue | ----->  | Write |
 *   | Wait  |               |             +--------+         +-------+
 *   +-------+               |
 *      |                    | TaskManager.ready() triggers
 *      +--------------------+ onResume(), re-enter Encode
 * </pre>
 *
 * <p>State transitions:</p>
 * <ul>
 *   <li>1. Start       → {@code onStart()}</li>
 *   <li>2. Wait        → {@code TaskManager.addTaskToWaitingQueue()} (paused when no available session)</li>
 *   <li>3. Resume      → {@code onResume()} (resumes when session becomes available)</li>
 *   <li>4. Encode      → {@code onEncode()} (encodes the request data)</li>
 *   <li>5. Submit      → {@code onSubmit()} (submits the task to the write queue)</li>
 *   <li>6. Write       → {@code Writer.write()} (writes to the socket output stream)</li>
 *   <li>7. Decode      → {@code onDecode()} (decodes the response packet)</li>
 *   <li>8. Complete    → {@code onSuccess()}, {@code onFailure()}, or {@code onCancel()}</li>
 * </ul>
 *
 * <p>Completion states (<b>{@code CompleteType}</b>):</p>
 * <ul>
 *   <li>{@link CompleteType#SUCCESS} → Successfully received and decoded the response</li>
 *   <li>{@link CompleteType#FAILURE} → Various errors (e.g., startup failure, encoding/decoding failure, connection issues, timeout)</li>
 *   <li>{@link CompleteType#CANCELED} → Task was canceled</li>
 * </ul>
 *
 * <p>⚠ Each task can only be executed once and cannot be re-executed after completion.</p>
 *
 * @param <T> The type of the response data.
 */
public class TaskImpl<T> implements OperableTask<T>, Runnable {
    // Indicates whether the task has already been executed. A task can only be executed once.
    private final AtomicBoolean executed = new AtomicBoolean();
    // The socket client associated with this task
    private final BaseSocketClient socketClient;
    // The request associated with this task
    private final Request<T> request;
    // The callback to handle task lifecycle events
    private final LifecycleCallback<T> callback;
    // Executor for encoding and decoding operations
    private final Executor codecExecutor;
    // Executor for task execution
    private final EasyExecutor mainExecutor;
    // Configuration options for the task
    private final Options options;
    // A unique identifier for the task, used to match requests with their responses.
    private final int taskId;
    // The type of the task.
    private final TaskType taskType;
    // The completion state of the task.
    private volatile CompleteType completeType = null;
    // Indicates whether the task is currently timing out.
    private boolean isTiming = false;
    // The encoded request data.
    private volatile byte[] data = new byte[0];
    // The response data.
    private volatile T response = null;
    // The error that occurred during task execution.
    private volatile Throwable error = null;
    // The task manager responsible for managing this task.
    private final TaskManager taskManager;
    //  The session associated with this task.
    private OperableSession session;
    // The writer responsible for writing the task to the socket.
    private Writer writer;

    /**
     * Constructs a new TaskImpl instance with the specified task ID, request, callback, and socket client.
     * This constructor initializes the task as a request type with no associated session.
     *
     * @param taskId       The unique identifier for the task.
     * @param request      The request object associated with the task.
     * @param callback     The callback to handle task lifecycle events.
     * @param socketClient The socket client associated with the task.
     */
    public TaskImpl(int taskId,
                    Request<T> request,
                    Callback<T> callback,
                    BaseSocketClient socketClient) {
        this(TaskType.REQUEST, taskId, request, callback, socketClient, null);
    }

    /**
     * Constructs a new TaskImpl instance with the specified parameters.
     * This constructor allows specifying the task type and an optional session.
     *
     * @param taskType     The type of the task (e.g., REQUEST, PULSE).
     * @param taskId       The unique identifier for the task.
     * @param request      The request object associated with the task.
     * @param callback     The callback to handle task lifecycle events.
     * @param socketClient The socket client associated with the task.
     * @param session      The session associated with the task, or null if not applicable.
     */
    public TaskImpl(TaskType taskType,
                    int taskId,
                    Request<T> request,
                    Callback<T> callback,
                    BaseSocketClient socketClient,
                    OperableSession session) {
        this.taskType = taskType;
        this.taskId = taskId;
        this.request = request;
        this.options = socketClient.getOptions();
        this.callback = new LifecycleCallbackWrapper<>(callback, this, socketClient);
        this.socketClient = socketClient;
        this.taskManager = socketClient.getTaskManager();
        this.mainExecutor = socketClient.getMainExecutor();
        this.codecExecutor = options.getCodecExecutor();
        this.session = session;
    }

    /**
     * Retrieves the unique identifier of the task.
     *
     * @return The task ID.
     */
    @Override
    public int getTaskId() {
        return taskId;
    }

    /**
     * Retrieves the type of the task.
     *
     * @return The type of the task, which can be one of the predefined TaskType values.
     */
    @Override
    public TaskType getTaskType() {
        return taskType;
    }

    /**
     * Retrieves the encoded request data.
     *
     * @return A byte array representing the encoded request data.
     */
    @Override
    public byte[] getData() {
        return data;
    }

    /**
     * Retrieves the response object of the task.
     *
     * @return The response data, or null if no response is available.
     */
    @Override
    public T getResponse() {
        return response;
    }

    /**
     * Retrieves the error that occurred during task execution.
     *
     * @return The throwable representing the error, or null if no error occurred.
     */
    @Override
    public Throwable getError() {
        return error;
    }

    /**
     * Checks whether the task has been completed.
     *
     * @return True if the task is completed, false otherwise.
     */
    @Override
    public boolean isCompleted() {
        return completeType != null;
    }

    /**
     * Checks whether the task was executed successfully.
     *
     * @return True if the task completed successfully, false otherwise.
     */
    @Override
    public boolean isSuccess() {
        return completeType == CompleteType.SUCCESS;
    }

    /**
     * Checks whether the task execution failed.
     *
     * @return True if an error occurred, false otherwise.
     */
    @Override
    public boolean isFailure() {
        return completeType == CompleteType.FAILURE;
    }

    /**
     * Checks whether the task was canceled.
     *
     * @return True if the task was canceled, false otherwise.
     */
    @Override
    public boolean isCanceled() {
        return completeType == CompleteType.CANCELED;
    }

    /**
     * Retrieves the request object associated with the task.
     *
     * @return The request object.
     */
    @Override
    @NonNull
    public Request<T> request() {
        return request;
    }

    /**
     * Executes the task's timeout logic. This method is called when the task times out.
     */
    @Override
    public void run() {
        onTimeout();
    }

    /**
     * Cancels the task. If the task is running, it attempts to interrupt execution.
     * If the task has not started, it marks the task as not to be executed.
     */
    @Override
    public void cancel() {
        if (isCompleted()) return;
        mainExecutor.execute(this::onCancel);
    }

    /**
     * Handles the cancellation logic for the task.
     * This method ensures the task is marked as canceled and notifies the callback.
     */
    private void onCancel() {
        if (!isCompleted()) {
            if (isTiming) {
                isTiming = false;
                mainExecutor.remove(this);
            }
            taskManager.cancelTask(this);
            if (writer != null) writer.cancel(this);
            completeType = CompleteType.CANCELED;
            callback.onCanceled();
        }
    }

    /**
     * Executes the task. This method ensures the task is only executed once.
     * If the task has already been executed, an exception is thrown.
     */
    @Override
    public void execute() {
        if (executed.compareAndSet(false, true)) {
            mainExecutor.execute(this::onStart);
        } else {
            throw new IllegalStateException("Task is already executed");
        }
    }

    /**
     * Handles the start logic for the task. This method initializes the task and
     * transitions it to the appropriate state based on the socket client's availability.
     */
    private void onStart() {
        if (isCompleted()) return;
        BaseSocketClient socketClient = this.socketClient;
        if (socketClient.isShutdown()) {
            Throwable t = EasyException.create(ErrorCode.SHUTDOWN, ErrorType.SYSTEM,
                    socketClient.suffix, "Socket client is shutdown");
            onError(t);
        } else {
            callback.onStart();
            taskManager.addTask(this);
            if (taskType == TaskType.REQUEST) {
                socketClient.start();
                if (socketClient.isAvailable()) {
                    this.session = Objects.requireNonNull(socketClient.getSession());
                    codecExecutor.execute(this::onEncode);
                } else {
                    taskManager.addTaskToWaitingQueue(this);
                    callback.onWait();
                }
            } else {
                codecExecutor.execute(this::onEncode);
            }
        }
    }

    /**
     * Resumes the task. This method is called when the task is paused and the session becomes available.
     */
    @Override
    public void onResume() {
        if (isCompleted()) return;
        this.session = Objects.requireNonNull(socketClient.getSession());
        callback.onResume();
        codecExecutor.execute(this::onEncode);
    }

    /**
     * Encodes the request message. This is executed on the codec thread.
     * If encoding fails, the task is marked as failed and the error is reported.
     */
    private void onEncode() {
        callback.onEncodeStart();
        try {
            data = request.encode(taskId);
            if (data.length == 0) {
                Throwable t = EasyException.create(ErrorCode.REQUEST_DATA_EMPTY,
                        ErrorType.TASK, socketClient.suffix, "Request data is empty");
                callback.onEncodeFailure(t);
                mainExecutor.execute(() -> {
                    taskManager.removeTask(this);
                    onError(t);
                });
            } else {
                callback.onEncodeSuccess();
                mainExecutor.execute(this::onSubmit);
            }
        } catch (Throwable t) {
            callback.onEncodeFailure(t);
            mainExecutor.execute(() -> {
                taskManager.removeTask(this);
                onError(t);
            });
        }
    }

    /**
     * Submits the task to the writer for execution.
     * This method is called after the request data has been successfully encoded.
     */
    private void onSubmit() {
        if (isCompleted()) return;
        writer = Objects.requireNonNull(session.getWriter());
        writer.submit(this);
    }

    /**
     * Notifies the callback that the task has started sending data.
     */
    @Override
    public void onSendStart() {
        callback.onSendStart();
    }

    /**
     * Notifies the callback that the task has successfully sent data.
     * This method also starts the timeout timer for the task.
     */
    @Override
    public void onSendSuccess() {
        callback.onSendSuccess();
        if (isCompleted()) return;
        isTiming = true;
        mainExecutor.schedule(this, options.getRequestTimeOutInMills());
    }

    /**
     * Handles the failure of sending data. This method notifies the callback
     * and marks the task as failed.
     *
     * @param t The throwable representing the failure.
     */
    @Override
    public void onSendFailure(@NonNull Throwable t) {
        callback.onSendFailure(t);
        taskManager.removeTask(this);
        onError(t);
    }

    /**
     * Handles the timeout logic for the task. This method is called when the task
     * exceeds the allowed timeout duration.
     */
    private void onTimeout() {
        isTiming = false;
        taskManager.removeTask(this);
        Throwable t = EasyException.create(ErrorCode.RESPONSE_TIME_OUT, ErrorType.TASK,
                socketClient.suffix, "Response time out");
        onError(t);
    }

    /**
     * Handles the receipt of a packet. This method decodes the packet and processes the response.
     *
     * @param packet The packet received from the server.
     */
    @Override
    public void onPacketReceived(Packet packet) {
        mainExecutor.remove(this);
        callback.onPacketReceived(packet);
        codecExecutor.execute(() -> onDecode(packet));
    }

    /**
     * Decodes the received packet. This method is executed on the codec thread.
     * If decoding is successful, the task is marked as successful and the response is processed.
     *
     * @param packet The packet to decode.
     */
    private void onDecode(Packet packet) {
        callback.onDecodeStart();
        try {
            T result = request.decode(packet);
            callback.onDecodeSuccess();
            mainExecutor.execute(() -> onSuccess(result));
        } catch (Throwable t) {
            callback.onDecodeFailure(t);
            mainExecutor.execute(() -> onError(t));
        }
    }

    /**
     * Marks the task as successful and notifies the callback with the response.
     *
     * @param res The response object.
     */
    private void onSuccess(@NonNull T res) {
        if (!isCompleted()) {
            response = res;
            completeType = CompleteType.SUCCESS;
            callback.onSuccess(res);
        }
    }

    /**
     * Handles errors that occur during task execution. This method marks the task
     * as failed and notifies the callback with the error.
     *
     * @param t The throwable representing the error.
     */
    @Override
    public void onError(@NonNull Throwable t) {
        if (!isCompleted()) {
            if (isTiming) {
                isTiming = false;
                mainExecutor.remove(this);
            }
            this.error = t;
            completeType = CompleteType.FAILURE;
            callback.onFailure(t);
        }
    }

    /**
     * Enum representing the completion state of the task.
     */
    private enum CompleteType {
        // Successfully received a response
        SUCCESS,
        // An error occurred
        FAILURE,
        // Task was canceled
        CANCELED,
    }
}
