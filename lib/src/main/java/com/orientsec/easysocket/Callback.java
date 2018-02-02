package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.EasyException;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2017/12/26 16:54
 * Author: Fredric
 * coding is art not science
 * <p>
 * 请求结果回调
 */

public interface Callback<T> {
    /**
     * 成功回调
     *
     * @param res 响应消息
     */
    void onSuccess(T res);

    /**
     * 成功回调
     */
    void onSuccess();

    /**
     * 失败回调
     *
     * @param e 异常
     */
    void onError(Exception e);

    /**
     * 取消回调
     */
    void onCancel();
}
