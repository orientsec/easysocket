package com.orientsec.easysocket.inner.blocking;

import com.orientsec.easysocket.Message;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.Protocol;
import com.orientsec.easysocket.inner.AbstractConnection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2018/02/07 08:58
 * Author: Fredric
 * coding is art not science
 */
class Authorize {
    private Protocol protocol;

    private CountDownLatch countDownLatch;

    private Options options;

    private volatile boolean authorized;

    Authorize(AbstractConnection connection) {
        options = connection.options();
        protocol = options.getProtocol();
    }

    boolean waitForAuthorize() {
        countDownLatch = new CountDownLatch(1);
        try {
            countDownLatch.await(options.getRequestTimeOut(), TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        return authorized;
    }

    void onAuthorize(Message message) {
        if (!authorized) {
            authorized = protocol.authorize(message.getBody());
            countDownLatch.countDown();
        }
    }
}
