package com.producer;

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
    public static final String brokerIP = "127.0.0.1";

    private void start() throws Exception {

        Socket socket = new Socket(brokerIP, brokerPort);

        OutputStream os = socket.getOutputStream();


        RandomAccessFile raf = new RandomAccessFile("E:/1.exe", "r");

        int read = 0;

        byte[] content = new byte[1024 * 512];//512k
        int i = 0;
        while ((read = raf.read(content)) != -1) {

            // 消息头
            os.write(read);

            //body
            os.write(content);

            System.out.println("消息序号为：" + ++i);

            raf.read(content);
        }


        System.out.println("ok");


    }

    public static void main(String[] args) throws Exception {


        new FileProducer().start();
    }
}
