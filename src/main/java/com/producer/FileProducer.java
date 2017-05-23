package com.producer;

import com.util.ByteUtil;

import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.channels.Selector;

/**
 * Created by chen on 2016/8/23.
 */
public class FileProducer {

    /**
     * selector
     */
    private Selector selector;

    /**
     * brokerPort
     */
    private static int brokerPort = 9527;

    /**
     * brokerIP
     */
    public static final String brokerIP = "10.8.6.87";

    private void start() throws Exception {

        Socket socket = new Socket(brokerIP, brokerPort);

        OutputStream os = socket.getOutputStream();

        RandomAccessFile raf = new RandomAccessFile("E:/1.xlsx", "r");

        int read;

        byte[] content = new byte[1024];//1k
        int i = 0;
        while ((read = raf.read(content)) != -1) {

            // 消息头
            os.write(ByteUtil.int2byte(read));

            //body
            os.write(content);

            System.out.println("消息序号为：" + ++i);
        }

        // socket.setSoLinger(true, 0);
        socket.close();

        System.out.println("finished");

    }

    public static void main(String[] args) throws Exception {

        new FileProducer().start();
    }
}
