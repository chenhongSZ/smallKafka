package com.broker;

import com.queue.FileMessageQueue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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
     * body_length
     */
    private static int bodyLength = -1;

    // 创建读取数据缓冲器
    private ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 + 4);

    public static void main(String[] args) throws Exception {

        new Broker().start(port);

        queue = new FileMessageQueue();
        queue.init();

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

    public void listen() throws IOException {

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

    private void handleAcceptable(SelectionKey key) throws IOException, ClosedChannelException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        // 获得客户端连接通道
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);

        // 在与客户端连接成功后，为客户端通道注册SelectionKey.OP_READ事件。
        channel.register(selector, SelectionKey.OP_READ);
    }

    private void handleReadable(SelectionKey key) throws IOException {

        // 有可读数据事件
        // 获取客户端传输数据可读取消息通道。
        SocketChannel channel = (SocketChannel) key.channel();
        int read = channel.read(buffer);

        while (buffer.remaining() > 0) {

        }


        headLength += read;

        if (read >= 4) {

        }

        if (read > 0) {
            buffer.getInt();

            byte[] data = buffer.array();
            String message = new String(data, 0, read);

            System.out.println(message);
        }
        else {
            channel.close();
        }
    }

}
