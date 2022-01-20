package com.amit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;


public class Main {
    static int numberOfConnection = 0;
    static int numberOfBackStreamConnections = 0;

    public static void main(String[] args) throws IOException {

            Selector selector = Selector.open();
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress("localhost", 10000));
            serverSocket.configureBlocking(false);

            HashMap<String, Object> attr = new HashMap<>();
            attr.put("channel_type","server");
            attr.put("back_connection", "");
            attr.put("last_used_time", java.time.Instant.now().getEpochSecond());


            SelectionKey selectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            selectionKey.attach(attr);



            while(true){

                selector.select();
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectionKeySet.iterator();
                System.out.println("Iterator set count is " + selectionKeySet.size());
                while(iter.hasNext()){

                    selectionKey = iter.next();
                    iter.remove();
                    String channelType = "";

                    System.out.println("Notification from " + selectionKey.channel());
                    System.out.println("Number of connections accepted are " + numberOfConnection);
                    System.out.println("Number of backstream connections accepted are " + numberOfBackStreamConnections);

                    if( selectionKey.isValid() && selectionKey.attachment() != null){
                        attr = (HashMap)selectionKey.attachment();
                        channelType = (String)attr.get("channel_type");
                        System.out.println( "attachment is " + attr);
                        System.out.println("Channel type is " + channelType);

                    }

                    if(selectionKey.isValid() && selectionKey.isAcceptable() && channelType.equals("server")){
                        System.out.println("Server Connection is accepted");

                        SocketChannel connection = serverSocket.accept();
                        numberOfConnection = numberOfConnection + 1;
                        connection.configureBlocking(false);

                        HashMap<String, Object> x = new HashMap<>();
                        x.put("channel_type","server");
                        x.put("back_connection", "");
                        x.put("last_used_time", java.time.Instant.now().getEpochSecond());


                        SelectionKey selectionKey1 = connection.register(selector, SelectionKey.OP_READ);
                        selectionKey1.attach(x);
                    }


                    if(selectionKey.isValid() && selectionKey.isReadable() && channelType.equals("server")){

                        System.out.println("Server Channel is readable");
                        SocketChannel connection = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);

                        try{
                            int bytesRead;
                            bytesRead = connection.read(buffer);
                            if(connection.read(buffer) < 0){
                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();

                                long lastUsed = (long)yy.get("last_used_time");
                                long now = java.time.Instant.now().getEpochSecond();

                                long timeElapsed = now - lastUsed;

                                if(timeElapsed > 15 ){
                                    System.out.println("15 seconds elapsed without any data transfer, terminating downstream connection");
                                    connection.close();
                                }

                                System.out.println("Client has stopped sending the data but not closed the connection");
                                continue;
                            }
                            else{

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();
                                yy.put("last_used_time",java.time.Instant.now().getEpochSecond());
                                selectionKey.attach(yy);


                                //After reading the request from downstream, make a connection with upstream

                                HashMap<String, Object> attrClient = new HashMap<>();
                                attrClient.put("channel_type","client");
                                attrClient.put("back_connection", connection);
                                attrClient.put("last_used_time", java.time.Instant.now().getEpochSecond());

                                SocketChannel clientSocket = SocketChannel.open();
                                clientSocket.connect(new InetSocketAddress("localhost", 80));
                                clientSocket.configureBlocking(false);
                                numberOfBackStreamConnections = numberOfConnection + 1;
                                System.out.println("Apache socket is " + clientSocket);
                                SelectionKey selectionKey2 = clientSocket.register(selector,  SelectionKey.OP_READ);
                                selectionKey2.attach(attrClient);

                                System.out.println("Downstream connection attached " + attrClient.get("back_connection"));

                                while(bytesRead > 0 ){

                                    buffer.flip();
                                    clientSocket.write(buffer);
                                    buffer.clear();
                                    bytesRead = connection.read(buffer);

                                }

                            }
                        }
                        catch(Exception e){
                            connection.close();
                            selectionKey.cancel();
                            e.printStackTrace();
                        }

                    }

                    if(selectionKey.isValid() && selectionKey.isReadable() && channelType.equals("client")) {

                        System.out.println("Client Channel is readable");
                        SocketChannel connection = (SocketChannel) selectionKey.channel();
                        System.out.println("Apache socket for readable " + connection);
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        System.out.println("Data from apache is " + new String(buffer.array()));

                        try {

                            int bytesRead;
                            bytesRead = connection.read(buffer);
                            if( bytesRead < 0) {

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();

                                long lastUsed = (long)yy.get("last_used_time");
                                long now = java.time.Instant.now().getEpochSecond();

                                long timeElapsed = now - lastUsed;

                                if(timeElapsed > 15 ){
                                    System.out.println("15 seconds elapsed without any data transfer, terminating upstream connection");
                                    connection.close();
                                }


                                System.out.println("Upstream has stopped sending the data but has not closed the connection");
                                continue;

                            } else {

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();
                                yy.put("last_used_time",java.time.Instant.now().getEpochSecond());
                                selectionKey.attach(yy);

                                SocketChannel conn = null;
                                try {

                                    if (selectionKey.attachment() != null) {
                                        attr = (HashMap<String, Object>) selectionKey.attachment();
                                        selectionKey.attach(null);
                                        conn = (SocketChannel) attr.get("back_connection");

                                        while(bytesRead > 0 ){

                                            buffer.flip();
                                            conn.write(buffer);
                                            buffer.clear();
                                            bytesRead = connection.read(buffer);

                                        }


                                        System.out.println("Sending data back to " + conn);


                                    } else {
                                        System.out.println("No one to send data back");
                                    }
                                }
                                catch(Exception exception) {
                                    System.out.println("Error while writing to client socket" + conn);
                                    exception.printStackTrace();
                                    conn.close();
                                }

                            }
                            connection.close();
                        }
                        catch(Exception e) {
                            System.out.println("Exception while reading from apache connection" + connection);
                            e.printStackTrace();
                            connection.close();

                            System.out.println("If apache ReSET the connection, then ELB close the client connection");
                            if (selectionKey.attachment() != null) {
                                attr = (HashMap<String, Object>) selectionKey.attachment();
                                selectionKey.attach(null);
                                SocketChannel conn = (SocketChannel) attr.get("back_connection");
                                conn.close();
                            }

                        }

                    }


                    }
                }

            }
        }

