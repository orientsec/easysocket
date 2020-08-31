package com.orientsec.easysocket.task;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Callback;
import com.orientsec.easysocket.Request;

public interface TaskFactory<T> {
    /**
     * 创建一个请求任务。
     *
     * @param request  发往服务端的请求。
     * @param callback 结果回调。
     * @return 可执行任务。
     */
    @NonNull
    <RE> Task<T, RE> buildTask(@NonNull Request<T, RE> request, @NonNull Callback<RE> callback);
}
