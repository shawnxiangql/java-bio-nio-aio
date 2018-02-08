package com.xql.nio;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

/**
 * Created by xql on 18-2-8.
 */
public class NioClient {

    public static void main(String[] args) {
        int clientCount = 10;
        String host = "localhost";
        int port = 8000;

        for (int i = 0; i < clientCount; i++) {
            new Thread(() -> send(host, port)).start();
        }
    }

    private static void send(String host, int port) {
        SocketChannel socketChannel = null;
        try {
            SocketAddress address = new InetSocketAddress(host, port);

            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            Selector selector = SelectorProvider.provider().openSelector();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(address);

            while (selector.isOpen()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    if (selectionKey.isConnectable()) {
                        connect(selector, selectionKey);
                    } else if (selectionKey.isReadable()) {
                        read(selector, selectionKey);
                    } else if (selectionKey.isWritable()) {
                        write(selectionKey);
                    }
                    iterator.remove();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void write(SelectionKey selectionKey) {
        try {
            ByteArrayOutputStream attachment = (ByteArrayOutputStream) selectionKey.attachment();
            if (attachment != null) {
                byte[] arr = attachment.toByteArray();
                String message = new String(arr, "UTF-8");
                System.out.println("receive callback: " + message);
                selectionKey.channel().close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void read(Selector selector, SelectionKey selectionKey) {
        try {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            channel.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(20);
            int length = channel.read(buffer);
            if (length > 0) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(buffer.array());
                selectionKey.attach(byteArrayOutputStream);
//                channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                channel.register(selector, SelectionKey.OP_READ);
            } else {
                channel.register(selector, SelectionKey.OP_WRITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void connect(Selector selector, SelectionKey selectionKey) {
        try {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            channel.configureBlocking(false);
            channel.write(ByteBuffer.wrap((Thread.currentThread().getName() + " say: hello server! ").getBytes("UTF-8")));
            channel.register(selector, SelectionKey.OP_READ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
