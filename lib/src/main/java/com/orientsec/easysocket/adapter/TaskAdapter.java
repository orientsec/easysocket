package com.orientsec.easysocket.adapter;

import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;

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
     * @param <T>     请求返回类型
     * @return Observable
     */
    public static <T, REQ, RES> Observable<RES>
    buildObservable(Connection<T> connection, Request<T, REQ, RES> request) {
        TaskObservable<RES> observable = new TaskObservable<>();
        Task<RES> task = connection.buildTask(request, observable);
        observable.setTask(task);
        return observable;
    }

}
