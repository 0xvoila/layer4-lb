package com.amit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;



public class LoadBalancer extends Thread{
    static int numberOfConnection = 0;
    static int numberOfBackStreamConnections = 0;
    UpStreamManagement upStreamManagement;
    private static Logger logger = LogManager.getLogger(LoadBalancer.class);

    LoadBalancer(UpStreamManagement upStreamManagement){
        this.upStreamManagement = upStreamManagement;
    }

    public void run() {
        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverSocket = ServerSocketChannel.open();

            serverSocket.bind(new InetSocketAddress("localhost", 10000));
            serverSocket.configureBlocking(false);

            HashMap<String, Object> attr = new HashMap<>();
            attr.put("channel_type", "down_stream");
            attr.put("overload_connection", "");
            attr.put("last_used_time", java.time.Instant.now().getEpochSecond());

            SelectionKey selectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            selectionKey.attach(attr);


            while (true) {

                selector.select();
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectionKeySet.iterator();
                while (iter.hasNext()) {

                    selectionKey = iter.next();
                    iter.remove();
                    String channelType = "";


                    if (selectionKey.isValid() && selectionKey.attachment() != null) {
                        attr = (HashMap) selectionKey.attachment();
                        channelType = (String) attr.get("channel_type");

                    }

                    if (selectionKey.isValid() && selectionKey.isAcceptable() && channelType.equals("down_stream")) {


                        logger.info("down stream connection is accepted");

                        SocketChannel downstreamConnection = serverSocket.accept();
                        numberOfConnection = numberOfConnection + 1;
                        downstreamConnection.configureBlocking(false);

                        HashMap<String, Object> x = new HashMap<>();
                        x.put("channel_type", "down_stream");
                        x.put("overload_connection", "");
                        x.put("last_used_time", java.time.Instant.now().getEpochSecond());


                        SelectionKey selectionKey1 = downstreamConnection.register(selector, SelectionKey.OP_READ);
                        selectionKey1.attach(x);
                    }


                    if (selectionKey.isValid() && selectionKey.isReadable() && channelType.equals("down_stream")) {

                        logger.info("down stream channel is readable");
                        SocketChannel downstreamConnection = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);

                        try {
                            int bytesRead;
                            bytesRead = downstreamConnection.read(buffer);
                            if (downstreamConnection.read(buffer) < 0) {
                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();

                                long lastUsed = (long) yy.get("last_used_time");
                                long now = java.time.Instant.now().getEpochSecond();

                                long timeElapsed = now - lastUsed;

                                if (timeElapsed > 15) {
                                    logger.debug("15 seconds elapsed without any data transfer, terminating downstream connection");
                                    downstreamConnection.close();
                                }
                                else{
                                    logger.debug("down stream has stopped sending the data but not closed the connection");
                                }
                                
                                continue;
                            } else {

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();
                                yy.put("last_used_time", java.time.Instant.now().getEpochSecond());
                                selectionKey.attach(yy);


                                //After reading the request from downstream, make a connection with upstream

                                HashMap<String, Object> attrClient = new HashMap<>();
                                attrClient.put("channel_type", "upstream");
                                attrClient.put("overload_connection", downstreamConnection);
                                attrClient.put("last_used_time", java.time.Instant.now().getEpochSecond());

                                SocketChannel upstreamConnection = null;
                                UpStream upStream = this.upStreamManagement.getNextUpStream();
                                try{

                                    //Here Get the upstream from UpStreamManagementService
                                    if(upStream == null){
                                        logger.warn("no upstream is available");
                                        continue;
                                    }

                                    logger.debug("upstream for next request to serve is " + upStream.serverAddress + ":" + upStream.serverPort);
                                    upstreamConnection = SocketChannel.open();
                                    upstreamConnection.connect(new InetSocketAddress(upStream.getServerAddress(), upStream.getServerPort()));
                                    upstreamConnection.configureBlocking(false);
                                }
                                catch(Exception exception){
                                    upstreamConnection.close();
                                    downstreamConnection.close();
                                    logger.fatal("Unable to reach upstream server");
                                    logger.warn("closing downstream socket connection");
                                    logger.warn("closing upstream socket connection as well");
                                    logger.fatal(upStream.serverAddress + " " + upStream.serverPort);
                                    continue;
                                }

                                numberOfBackStreamConnections = numberOfConnection + 1;
                                SelectionKey selectionKey2 = upstreamConnection.register(selector, SelectionKey.OP_READ);
                                selectionKey2.attach(attrClient);

                                logger.info("downstream connection overloaded on upstream " + attrClient.get("overload_connection"));

                                while (bytesRead > 0) {

                                    buffer.flip();
                                    upstreamConnection.write(buffer);
                                    buffer.clear();
                                    bytesRead = downstreamConnection.read(buffer);

                                }

                            }
                        } catch (Exception e) {
                            downstreamConnection.close();
                            selectionKey.cancel();
                            e.printStackTrace();
                        }

                    }

                    if (selectionKey.isValid() && selectionKey.isReadable() && channelType.equals("upstream")) {

                        logger.info("upstream channel is readable");
                        SocketChannel upstreamConnection = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);

                        try {

                            int bytesRead;
                            bytesRead = upstreamConnection.read(buffer);


                            if (bytesRead < 0) {

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();

                                long lastUsed = (long) yy.get("last_used_time");
                                long now = java.time.Instant.now().getEpochSecond();

                                long timeElapsed = now - lastUsed;

                                if (timeElapsed > 15) {
                                    logger.debug("15 seconds elapsed without any data transfer, terminating upstream connection");
                                    upstreamConnection.close();
                                }


                                logger.warn("Upstream has stopped sending the data but has not closed the connection");
                                continue;

                            } else {

                                HashMap<String, Object> yy = (HashMap<String, Object>) selectionKey.attachment();
                                yy.put("last_used_time", java.time.Instant.now().getEpochSecond());
                                selectionKey.attach(yy);

                                SocketChannel downstreamConnection = null;
                                try {

                                    if (selectionKey.attachment() != null) {
                                        attr = (HashMap<String, Object>) selectionKey.attachment();
                                        selectionKey.attach(null);
                                        downstreamConnection = (SocketChannel) attr.get("overload_connection");

                                        while (bytesRead > 0) {
                                            logger.info("number of bytes read is " + bytesRead);
                                            logger.info("data from upstream is " + new String(buffer.array()));
                                            buffer.flip();
                                            downstreamConnection.write(buffer);
                                            buffer.clear();
                                            bytesRead = upstreamConnection.read(buffer);

                                        }

                                        logger.info("sending data back to downstream connection " + downstreamConnection);
                                        downstreamConnection.close();

                                    } else {
                                        logger.debug("no data to send back to downstream");
                                    }
                                } catch (Exception exception) {
                                    logger.error("error while writing to client socket" + downstreamConnection);
                                    exception.printStackTrace();
                                    downstreamConnection.close();
                                }

                            }
                            upstreamConnection.close();
                        } catch (Exception e) {
                            logger.error("Exception while reading from upstream connection" + upstreamConnection);
                            e.printStackTrace();
                            upstreamConnection.close();

                            logger.error("upstream has reset the connection closing load balancer connection");
                            if (selectionKey.attachment() != null) {
                                attr = (HashMap<String, Object>) selectionKey.attachment();
                                selectionKey.attach(null);
                                SocketChannel conn = (SocketChannel) attr.get("overload_connection");
                                conn.close();
                            }

                        }

                    }


                }
            }

        } catch (Exception exception) {
            logger.fatal("Exception has occured");
            exception.printStackTrace();
            logger.fatal(exception.getMessage());
        }
    }
}