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

public interface Callback<RESPONSE> {
    /**
     * 请求开始执行回调
     */
    void onStart();

    /**
     * 成功回调
     *
     * @param res 响应
     */
    void onSuccess(RESPONSE res);

    /**
     * 失败回调
     *
     * @param e 异常
     */
    void onError(EasyException e);

    /**
     * 取消回调
     */
    void onCancel();

    class EmptyCallback<R> implements Callback<R> {

        @Override
        public void onStart() {

        }

        @Override
        public void onSuccess(R res) {

        }

        @Override
        public void onError(EasyException e) {

        }

        @Override
        public void onCancel() {

        }
    }
}
