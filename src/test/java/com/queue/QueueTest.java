package com.queue;

import com.base.Message;
import org.junit.Test;

/**
 * Created by chen on 2016/8/30.
 */
public class QueueTest {


    @Test
    public void read() throws Exception {
        FileMessageQueue fsq = new FileMessageQueue();
        fsq.init();


        Message result = fsq.read();
        int i = 0;

        while (result != null) {
            System.out.println(new String(result.getContent(), "UTF-8") + (i++));
            result = fsq.read();

        }

    }

    @Test
    public void write() throws Exception {
        FileMessageQueue fsq = new FileMessageQueue();
        fsq.init();

        Message msg = new Message();

        byte[] content = "helloWorld".getBytes("utf-8");

        msg.setLength(content.length);
        msg.setContent(content);

        for (int i = 0; i < 1000; i++) {
            fsq.write(msg);
        }

        System.out.println("ok");

    }


}
