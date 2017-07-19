package com.gj.animalauto.message;

/**
 * Created by liorsaar on 2015-05-17
 */
public interface GjMessageListener {
    void onMessage(GjMessage message);
    void incoming(byte[] bytes);
}
