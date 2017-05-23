package com.consumer;

import com.util.ByteUtil;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.channels.Selector;

/**
 * Created by chen on 2016/8/23.
 */
public class FileConsumer {

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

        File file = new File("E:/target.xlsx");

        if (file.exists()) {
            file.delete();
        }

        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        Socket socket = new Socket(brokerIP, brokerPort);

        OutputStream os = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        // 负数为 为consuemr
        os.write(ByteUtil.int2byte(-1));

        int read;
        int readed = 0;
        int length = 0;
        boolean readHead = false;

        byte[] bytes = new byte[4 + 1024];

        int count = 0;

        while ((read = in.read(bytes, readed, bytes.length - readed)) != -1) {

            readed += read;

            // 头装满了没
            if (readed >= 4 && !readHead) {
                length = ByteUtil.byte2int(bytes);
                readHead = true;
            }

            if (length < 0) {
                // 这个文件结束了
                socket.close();
            }

            //身子也被装满了
            if (readHead && readed == (length + 4)) {
                raf.write(bytes, 4, readed - 4);

                readHead = false;
                length = 0;
                readed = 0;

                System.out.println("recieve,count=" + ++count);
            }
        }

        System.out.println("ok");

    }

    public static void main(String[] args) throws Exception {

        new FileConsumer().start();
    }
}
