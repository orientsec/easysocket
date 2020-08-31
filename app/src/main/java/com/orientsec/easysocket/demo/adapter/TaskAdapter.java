package com.orientsec.easysocket.demo.adapter;

import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.task.Task;

import io.reactivex.Observable;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.utils
 * Time: 2017/12/27 10:33
 * Author: Fredric
 * coding is art not science
 * <p>
 * 适配器
 */
public class TaskAdapter {
    /**
     * 将Task转换为Observable
     *
     * @param request 请求
     * @param <R>     请求返回类型
     * @return Observable
     */
    public static <R> Observable<R>
    buildObservable(Connection connection, Request<R> request) {
        TaskObservable<R> observable = new TaskObservable<>();
        Task<R> task = connection.buildTask(request, observable);
        observable.setTask(task);
        return observable;
    }

}
