package com.orientsec.easysocket.push;

/**
 * Product: framework
 * Package: winner.tcp
 * Time: 2017/6/8 12:47
 * Author: Fredric
 * coding is art not science
 */

public interface PushListener<T> {
    void onPush(T t);
}
