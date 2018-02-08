package com.xql.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by xql on 18-2-8.
 */
public class EchoClient {

    public static void main(String[] args) {
        int clientCount = 10;
        String host = "localhost";
        int port = 8000;

        for (int i = 0; i < clientCount; i++) {
            new Thread(() -> send(host, port)).start();
        }
    }

    private static void send(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            Thread.sleep((long) (Math.random() * 2000));
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            printWriter.println(Thread.currentThread().getName() + " say: hello server! ");
            printWriter.flush();
            String line = bufferedReader.readLine();
            System.out.println("receive callback: " + line);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
