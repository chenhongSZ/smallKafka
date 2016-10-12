package com.producer;

import com.util.ByteUtil;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.Selector;


/**
 * Created by chen on 2016/8/23.
 */
public class Producer {

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

        String body = "1234";

        byte[] bytes = body.getBytes();

        // 先写消息头
        os.write(ByteUtil.int2byte(bytes.length));

        // 再写消息体

        os.write(bytes);

        os.flush();

        os.close();

        socket.close();

    }

    public static void main(String[] args) throws Exception {

        new Producer().start();
    }
}
