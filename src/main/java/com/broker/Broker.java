package com.broker;

import com.base.Message;
import com.queue.FileMessageQueue;
import com.util.ByteUtil;

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

    /**
     * hasFullHead 头部已经读满
     */
    private static boolean hasFullHead = false;

    // 创建读取数据缓冲器
    private ByteBuffer headBuffer = ByteBuffer.allocate(4);

    // 创建读取数据缓冲器
    private ByteBuffer bodyBuffer = null;

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
        serverChannel.socket().bind(new InetSocketAddress(port));

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

                }
                else if (key.isReadable()) {

                    System.out.println("isReadable");
                    handleReadable(key);

                }
                else if (key.isConnectable()) {

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
        channel.register(selector, SelectionKey.OP_READ);
    }

    private void handleReadable(SelectionKey key) throws Exception {

        // 有可读数据事件
        // 获取客户端传输数据可读取消息通道。

        SocketChannel channel = (SocketChannel) key.channel();

        if (!hasFullHead) {
            int read = channel.read(headBuffer);
            //并且缓存区的长度大于4(包头部分已经接受完毕)
            if (!headBuffer.hasRemaining()) {
                int bodyLength = ByteUtil.byte2int(headBuffer.array());

                //清空 后续使用
                headBuffer.clear();

                // body buffer
                bodyBuffer = ByteBuffer.allocate(bodyLength);
                hasFullHead = true;

            }
            else {
                return;
            }
        }
        else {

            if (bodyBuffer.hasRemaining()) {
                channel.read(bodyBuffer);
            }

            if (bodyBuffer.hasRemaining()) {
                return;

            }else {
                byte[] bodyArr = bodyBuffer.array();

                //清空 后续使用
                bodyBuffer.clear();

                // 写入队列
                queue.write(new Message(bodyArr.length, bodyArr));
            }


        }


    }

}
