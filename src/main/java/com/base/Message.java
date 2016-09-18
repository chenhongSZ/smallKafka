package com.base;

/**
 * Created by chen on 2016/8/29.
 */
public class Message {

    /**
     * 头长度， 目前只有内容长度字段
     */
    public final static int HEAD_SIZE = 4;

    /**
     * 长度
     */
    private int length;

    /**
     * 内容
     */
    private byte[] content;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
