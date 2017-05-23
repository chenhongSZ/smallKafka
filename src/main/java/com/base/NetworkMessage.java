package com.base;

import java.nio.ByteBuffer;

/**
 * Created by chen on 2016/8/29.
 */
public class NetworkMessage {
    /**
     * isFullHead 头部已经读满
     */
    private boolean isFullHead = false;

    // 创建读取数据缓冲器
    private ByteBuffer headBuffer = ByteBuffer.allocate(4);

    // 创建读取数据缓冲器
    private ByteBuffer bodyBuffer = null;

    public boolean isFullHead() {
        return isFullHead;
    }

    public void setFullHead(boolean fullHead) {
        isFullHead = fullHead;
    }

    public ByteBuffer getHeadBuffer() {
        return headBuffer;
    }

    public void setHeadBuffer(ByteBuffer headBuffer) {
        this.headBuffer = headBuffer;
    }

    public ByteBuffer getBodyBuffer() {
        return bodyBuffer;
    }

    public void setBodyBuffer(ByteBuffer bodyBuffer) {
        this.bodyBuffer = bodyBuffer;
    }
}
