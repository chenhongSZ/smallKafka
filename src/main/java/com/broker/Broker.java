package com.broker;

import com.base.Message;
import com.base.NetworkMessage;
import com.queue.FileMessageQueue;
import com.util.ByteUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by atomchen on 2016/8/23.
 */
public class Broker {

    /**
     * selector
     */
    private Selector selector;

    /**
     * port
     */
    private static int port = 9527;

    /**
     * stop
     */
    private static volatile boolean stop = false;

    /**
     * 文件队列
     */
    private static FileMessageQueue queue = null;

    public static void main(String[] args) throws Exception {

        queue = new FileMessageQueue();
        queue.init();

        new Broker().start(port);

    }

    private void start(int port) throws Exception {

        selector = Selector.open();

        // 获取一个ServerSocket通道
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress("10.8.6.87", port));

        // 将通道管理器与通道绑定，并为该通道注册SelectionKey.OP_ACCEPT事件，
        // 只有当该事件到达时，Selector.select()会返回，否则一直阻塞。
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("开始监听在" + port + "端口上");

        listen();

    }

    public void listen() throws Exception {

        // 使用轮询访问selector
        while (!stop) {
            // 当有注册的事件到达时，方法返回，否则阻塞。
            selector.select();

            // 获取selector中的迭代器，选中项为注册的事件
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // 删除已选key，防止重复处理
                iterator.remove();

                if (key.isAcceptable()) {
                    System.out.println("isAcceptable");
                    handleAcceptable(key);

                } else if (key.isReadable()) {

                    System.out.println("isReadable");
                    handleReadable(key);

                } else if (key.isConnectable()) {

                    System.out.println("isConnectable");
                    //handleConnectable(key);
                }

            }
        }
    }

    private void handleAcceptable(SelectionKey key) throws Exception {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        // 获得客户端连接通道
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);

        // 在与客户端连接成功后，为客户端通道注册SelectionKey.OP_READ事件。

        NetworkMessage networkMessage = new NetworkMessage();

        channel.register(selector, SelectionKey.OP_READ, networkMessage);
    }

    private void handleReadable(SelectionKey key) throws Exception {

        SocketChannel channel = (SocketChannel) key.channel();
        NetworkMessage networkMessage = (NetworkMessage) key.attachment();

        try {

            // 有可读数据事件
            // 获取客户端传输数据可读取消息通道。

            if (!networkMessage.isFullHead()) {
                int read = channel.read(networkMessage.getHeadBuffer());

                if (read == -1) {
                    channel.close();
                }

                //并且缓存区的长度大于4(包头部分已经接受完毕)
                if (!networkMessage.getHeadBuffer().hasRemaining()) {
                    int bodyLength = ByteUtil.byte2int(networkMessage.getHeadBuffer().array());

                    //清空 后续使用
                    networkMessage.getHeadBuffer().clear();
                    networkMessage.setFullHead(true);

                    if (bodyLength < 0) {

                        //订阅的
                        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + 1024);

                        Message message;
                        int count = 0;
                        while ((message = queue.read()) != null) {

                            byteBuffer.put(ByteUtil.int2byte(message.getLength()));
                            byteBuffer.put(message.getContent());

                            byteBuffer.flip();
                            //如果 result == 0  表明缓冲区已经满了，可以注册写事件
                            int result = channel.write(byteBuffer);
                            System.out.println("result:" + result);
                            byteBuffer.clear();

                            System.out.println("send,count=" + ++count);

                        }

                        byteBuffer.clear();
                        byteBuffer.put(ByteUtil.int2byte(-1));
                        System.out.println("send,count=-1");
                        byteBuffer.flip();
                        channel.write(byteBuffer);

                        networkMessage.setBodyBuffer(ByteBuffer.allocate(10));

                    } else {
                        // body buffer
                        networkMessage.setBodyBuffer(ByteBuffer.allocate(bodyLength));
                    }

                } else {
                    return;
                }
            } else {

                if (networkMessage.getBodyBuffer().hasRemaining()) {
                    int read = channel.read(networkMessage.getBodyBuffer());

                    if (read == -1) {
                        channel.close();
                    }
                }

                if (networkMessage.getBodyBuffer().hasRemaining()) {
                    return;

                } else {
                    byte[] bodyArr = networkMessage.getBodyBuffer().array();

                    //清空 后续使用
                    networkMessage.getBodyBuffer().clear();
                    networkMessage.getHeadBuffer().clear();
                    networkMessage.setFullHead(false);

                    // 写入队列
                    queue.write(new Message(bodyArr.length, bodyArr));
                }
            }

        } catch (IOException e) {
            channel.close();
            e.printStackTrace();
        }
    }

}
