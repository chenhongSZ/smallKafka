package com.queue;


import com.base.Message;
import com.base.QueueContext;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by chen on 2016/8/29.
 */
public class FileMessageQueue {

    /**
     * 队列状态信息
     */
    private QueueContext context = QueueContext.getInstance();

    /**
     * 当前读文件
     */
    private RandomAccessFile readBlock;

    /**
     * 当前写文件
     */
    private RandomAccessFile writeBlock;

    public FileMessageQueue() {
        context.init();
        init();
    }

    public void init() {
        try {
            // 文件句柄
            File file = new File(QueueContext.WORK_SPACE + context.getWriteBlock().get());

            if (!file.exists()) {
                file.createNewFile();
            }

            writeBlock = new RandomAccessFile(file, "rw");
            readBlock = new RandomAccessFile(QueueContext.WORK_SPACE + context.getReadBlock().get(), "r");


            // 跳到指定的位置
            readBlock.seek(context.getReadPointer().get());
            writeBlock.seek(context.getWritePointer().get());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void write(Message message) throws Exception {

        changeWriteBlockIfNessesary(message);

        int length = message.getLength();

        writeBlock.writeInt(length);
        writeBlock.write(message.getContent());

        //修改写指针
        context.getWritePointer().addAndGet(Message.HEAD_SIZE + length);
        context.hibernate();

    }

    private void changeWriteBlockIfNessesary(Message message) {

        // 本地没有就创建新文件
        File file = new File(QueueContext.WORK_SPACE + context.getWriteBlock().get());
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }


        // 如果装不下头和内容，就换下一个块
        if (!context.checkCapacity(Message.HEAD_SIZE + message.getLength())) {
            // 换writeBlock
            synchronized (writeBlock) {
                try {
                    writeBlock.close();

                    int newBlockNo = context.getWriteBlock().incrementAndGet();

                    file = new File(QueueContext.WORK_SPACE + newBlockNo);
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    writeBlock = new RandomAccessFile(file, "rw");
                    context.getWritePointer().set(0);
                    context.hibernate();

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Message read() throws Exception {

        Message message = new Message();

        boolean empty = changeReadBlockIfNessesary();

        if (empty) {
            return null;
        }

        // 读写在同一个文件上,并且读指针大于等于写指针，就没有东西可读了
        if (context.getReadBlock().get() == context.getWriteBlock().get() && context.getReadPointer().get() >=
                context.getWritePointer().get()) {
            return null;
        }

        int length = readBlock.readInt();

        byte[] content = new byte[length];
        int read = readBlock.read(content);

        //修改读指针
        context.getReadPointer().addAndGet(Message.HEAD_SIZE + length);

        if (read != length) {
            throw new RuntimeException("读取文件出错，救不活了");
        }

        message.setLength(length);
        message.setContent(content);

        return message;
    }

    private boolean changeReadBlockIfNessesary() throws Exception {

        int length = 0;

        boolean needToChange = false;

        try {
            length = readBlock.readInt();
        }
        catch (EOFException e) {
            e.printStackTrace();

            // 读到尽头，换下一个
            needToChange = true;
        }


        //就是后续没有了
        if (length == 0 || needToChange) {

            int newBlockNo = context.getReadBlock().incrementAndGet();
            int writeNo = context.getWriteBlock().get();

            //读到尽头了,就等
            if (newBlockNo > writeNo) {
                return true;
            }

            synchronized (readBlock) {
                try {

                    readBlock.close();
                    readBlock = new RandomAccessFile(QueueContext.WORK_SPACE + newBlockNo, "r");

                    //回到文件头
                    context.getReadPointer().set(0);
                    context.hibernate();

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            // 多读了4位，后退回去，外层还会读
            readBlock.seek(context.getReadPointer().get());
        }

        return false;
    }
}
