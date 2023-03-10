package com.orientsec.easysocket.demo.adapter;


import androidx.annotation.NonNull;

import com.orientsec.easysocket.request.Callback;
import com.orientsec.easysocket.task.Task;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.adapter
 * Time: 2018/02/08 09:02
 * Author: Fredric
 * coding is art not science
 */
class TaskObservable<R> extends Observable<R> implements Callback<R> {
    private Task<R> task;
    private Observer<? super R> observer;

    public void setTask(Task<R> task) {
        this.task = task;
    }

    @Override
    public void onSend() {

    }

    @Override
    public void onSuccess(@NonNull R res) {
        if (!task.isCanceled()) {
            try {
                observer.onNext(res);
                observer.onComplete();
            } catch (Throwable inner) {
                Exceptions.throwIfFatal(inner);
                RxJavaPlugins.onError(inner);
            }
        }
    }

    @Override
    public void onError(@NonNull Exception e) {
        if (!task.isCanceled()) {
            try {
                observer.onError(e);
            } catch (Throwable inner) {
                Exceptions.throwIfFatal(inner);
                RxJavaPlugins.onError(new CompositeException(e, inner));
            }
        }
    }

    @Override
    public void onCancel() {
        try {
            observer.onComplete();
        } catch (Throwable inner) {
            Exceptions.throwIfFatal(inner);
            RxJavaPlugins.onError(inner);
        }
    }


    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        this.observer = observer;
        observer.onSubscribe(new TaskDisposable(task));
        task.execute();
    }

    static final class TaskDisposable implements Disposable {
        private final Task<?> task;

        TaskDisposable(Task<?> task) {
            this.task = task;
        }

        @Override
        public void dispose() {
            task.cancel();
        }

        @Override
        public boolean isDisposed() {
            return task.isCanceled();
        }
    }
}