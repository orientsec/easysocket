package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.request.Request;

public interface TaskFactory {
    /**
     * 创建一个请求任务。
     *
     * @param request  发往服务端的请求。
     * @param callback 结果回调。
     * @return 可执行任务。
     */
    @NonNull
    <R extends T, T> Task<R> buildTask(@NonNull Request<R> request, @NonNull Callback<T> callback);

}
