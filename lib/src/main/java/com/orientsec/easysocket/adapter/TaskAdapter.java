package com.orientsec.easysocket.adapter;

import com.orientsec.easysocket.Task;

import io.reactivex.Observable;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.utils
 * Time: 2017/12/27 10:33
 * Author: Fredric
 * coding is art not science
 */
public class TaskAdapter {
    public static <T> Observable<T> toObservable(Task<T> task) {
        return new TaskObservable<>(task);
    }

}
