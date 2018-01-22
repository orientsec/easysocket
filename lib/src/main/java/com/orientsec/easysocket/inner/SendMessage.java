package com.orientsec.easysocket.inner;

import com.orientsec.easysocket.Message;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.inner
 * Time: 2018/01/17 11:15
 * Author: Fredric
 * coding is art not science
 */
public class SendMessage extends Message {
    private boolean valid = true;

    public void invalid() {
        valid = false;
    }

    public boolean isValid() {
        return valid;
    }
}
