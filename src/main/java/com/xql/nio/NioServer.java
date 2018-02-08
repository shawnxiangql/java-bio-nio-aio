package com.xql.nio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by xql on 18-2-8.
 */
public class NioServer {
    static Selector selector;
    static List<String> messageList = new ArrayList<>();

    public static void main(String[] args) {
        try {
            selector = SelectorProvider.provider().openSelector();
            SocketAddress address = new InetSocketAddress(8000);
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(address);
            SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        doAccept(key);
                    } else if (key.isValid() && key.isReadable()) {
                        doRead(key);
                    } else if (key.isValid() && key.isWritable()) {
                        doWrite(key);
                    }
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(24);
        try {
            channel.configureBlocking(false);
            int length = channel.read(buffer);
            ByteArrayOutputStream attachment = (ByteArrayOutputStream) key.attachment();
            if (length <= 0) {
                key.interestOps(SelectionKey.OP_WRITE);
                channel.finishConnect();
                selector.wakeup();
                return;
            }
            buffer.flip();
            attachment.write(buffer.array());
            key.attach(attachment);
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//            System.out.println("read buffer: " + buffer.limit());
            selector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void doWrite(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteArrayOutputStream attachment = (ByteArrayOutputStream) key.attachment();
            if (attachment == null) {
                return;
            }
            byte[] array = attachment.toByteArray();
            String message = new String(array, "UTF-8");
            System.out.println("receive message: " + message.trim());
            byte[] bytes = message.getBytes("UTF-8");
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            channel.write(buffer);
            channel.finishConnect();
            channel.close();
            selector.wakeup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doAccept(SelectionKey key) {
        try {
            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = channel.accept();
            socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(new ByteArrayOutputStream());
            InetAddress inetAddress = socketChannel.socket().getInetAddress();
            System.out.println("receive connect: " + inetAddress.getHostAddress());
            selector.wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
