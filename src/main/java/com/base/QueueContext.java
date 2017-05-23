package com.base;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chen on 2016/8/29.
 */
public class QueueContext {

    private final static QueueContext index = new QueueContext();

    /**
     * 每个文件块的大小
     */
    private final static int BLOCK_SIZE = 1024 * 64;

    /**
     * 每个文件块的大小
     */
    public final static String WORK_SPACE = "E:\\test\\";

    /**
     * 索引文件
     */
    public final static File INDEX_FILE = new File(WORK_SPACE + File.separator + "index");

    /**
     * 索引文件句柄
     */
    private RandomAccessFile raf;

    /**
     * 读指针
     */
    private AtomicInteger readPointer = new AtomicInteger(0);

    /**
     * 写指针
     */
    private AtomicInteger writePointer = new AtomicInteger(0);

    /**
     * 读文件
     */
    private AtomicInteger readBlock = new AtomicInteger(0);

    /**
     * 写文件
     */
    private AtomicInteger writeBlock = new AtomicInteger(0);

    /**
     * 从本地文件里读出数据
     */
    public void init() {
        try {

            if (!INDEX_FILE.exists()) {
                INDEX_FILE.createNewFile();
            }

            raf = new RandomAccessFile(INDEX_FILE, "rw");
            raf.seek(0);

            index.getReadBlock().set(raf.readInt());
            index.getWriteBlock().set(raf.readInt());
            index.getReadPointer().set(raf.readInt());
            index.getWritePointer().set(raf.readInt());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 落地到文件
     */
    public synchronized void hibernate() {

        try {
            raf.seek(0);

            raf.writeInt(index.getReadBlock().get());
            raf.writeInt(index.getWriteBlock().get());
            raf.writeInt(index.getReadPointer().get());
            raf.writeInt(index.getWritePointer().get());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public AtomicInteger getReadPointer() {
        return readPointer;
    }

    public static QueueContext getInstance() {
        return index;
    }

    public void setReadPointer(AtomicInteger readPointer) {
        this.readPointer = readPointer;
        hibernate();
    }

    public AtomicInteger getWritePointer() {
        return writePointer;
    }

    public void setWritePointer(AtomicInteger writePointer) {
        this.writePointer = writePointer;
        hibernate();
    }

    public AtomicInteger getReadBlock() {
        return readBlock;
    }

    public void setReadBlock(AtomicInteger readBlock) {
        this.readBlock = readBlock;
        hibernate();
    }

    public AtomicInteger getWriteBlock() {
        return writeBlock;
    }

    public void setWriteBlock(AtomicInteger writeBlock) {
        this.writeBlock = writeBlock;
        hibernate();
    }

    /**
     * 是否还有指定的容量
     *
     * @param length
     * @return
     */
    public boolean checkCapacity(int length) {
        return (BLOCK_SIZE - writePointer.get()) >= length;
    }

}
