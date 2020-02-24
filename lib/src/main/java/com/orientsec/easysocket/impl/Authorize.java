package com.orientsec.easysocket.impl;

import com.orientsec.easysocket.Packet;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.HeadParser;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner.blocking
 * Time: 2018/02/07 08:58
 * Author: Fredric
 * coding is art not science
 */
class Authorize<T> {
    private HeadParser<T> headParser;

    private CountDownLatch countDownLatch;

    private Options<T> options;

    private volatile boolean authorized;

    Authorize(AbstractConnection<T> connection) {
        options = connection.options();
        headParser = options.getHeadParser();
    }

    boolean waitForAuthorize() {
        countDownLatch = new CountDownLatch(1);
        try {
            countDownLatch.await(options.getRequestTimeOut(), TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        return authorized;
    }

    void onAuthorize(Packet<T> packet) {
        if (!authorized) {
            authorized = headParser.authorize(packet.getBody());
            countDownLatch.countDown();
        }
    }
}
