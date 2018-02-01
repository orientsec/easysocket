package com.orientsec.easysocket.utils;

import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.Request;
import com.orientsec.easysocket.Task;
import com.orientsec.easysocket.exception.EasyException;
import com.orientsec.easysocket.exception.SerializeException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.utils
 * Time: 2017/12/27 10:33
 * Author: Fredric
 * coding is art not science
 */
public class TaskAdapter {
    public static <T, R> Observable<R> buildObservable(Request<T, R> request, Connection connection) {
        return new TaskObservable<>(request, connection);
    }

    private static class TaskObservable<T, R> extends Observable<R> {
        private Request<T, R> delegate;
        private Task task;
        private volatile Observer<? super R> observer;

        TaskObservable(Request<T, R> request, Connection connection) {
            this.delegate = request;
            task = connection.buildTask(new AdapterRequest());
        }

        class AdapterRequest extends Request<T, R> {
            @Override
            public boolean isSendOnly() {
                return delegate.isSendOnly();
            }

            @Override
            public T getRequest() {
                return delegate.getRequest();
            }

            @Override
            public R getResponse() {
                return delegate.getResponse();
            }

            @Override
            public byte[] encode(T request) throws SerializeException {
                return delegate.encode(request);
            }

            @Override
            public R decode(byte[] response) throws SerializeException {
                return delegate.decode(response);
            }

            @Override
            public void onSuccess(R res) {
                if (!task.isCanceled() && observer != null) {
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
            public void onSuccess() {
                if (!task.isCanceled() && observer != null) {
                    try {
                        observer.onNext(null);
                        observer.onComplete();
                    } catch (Throwable inner) {
                        Exceptions.throwIfFatal(inner);
                        RxJavaPlugins.onError(inner);
                    }
                }
            }

            @Override
            public void onError(EasyException e) {
                if (!task.isCanceled() && observer != null) {
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
                if (observer != null) {
                    try {
                        observer.onComplete();
                        observer = null;
                    } catch (Throwable inner) {
                        Exceptions.throwIfFatal(inner);
                        RxJavaPlugins.onError(inner);
                    }
                }
            }
        }

        @Override
        protected void subscribeActual(Observer<? super R> observer) {
            this.observer = observer;
            observer.onSubscribe(new TaskDisposable(task));
            task.execute();
        }

        private final class TaskDisposable implements Disposable {
            private final Task task;

            TaskDisposable(Task task) {
                this.task = task;
            }

            @Override
            public void dispose() {
                observer = null;
                task.cancel();
            }

            @Override
            public boolean isDisposed() {
                return task.isCanceled();
            }
        }
    }


}
