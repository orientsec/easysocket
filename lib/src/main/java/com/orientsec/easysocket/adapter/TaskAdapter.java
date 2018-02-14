package com.orientsec.easysocket.adapter;

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
     * @param task 可执行任务
     * @param <T>  请求返回类型
     * @return Observable
     */
    public static <T> Observable<T> toObservable(Task<T> task) {
        return new TaskObservable<>(task);
    }

}
