package network;

import jdk.jfr.consumer.RecordedEvent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;


public class TCPServer {
    private HashMap<Integer, SocketChannel> sessions;
    private List<Integer> serverPorts;
    private List<Integer> clientPorts;
    private ReceivingManager receivingManager;
    private SendingManager sendingManager;
    private int port;

    private Selector selector;

    public TCPServer(List<Integer> serverPorts, List<Integer> clientPorts, int port, SendingManager sendingManager,
                     ReceivingManager receivingManager) {
        this.serverPorts = serverPorts;
        this.clientPorts = clientPorts;
        this.sessions = new HashMap<>();
        this.port = port;
        this.receivingManager = receivingManager;
        this.sendingManager = sendingManager;
    }


    private void reconnectToServers() throws IOException {
        for (var serverPort : serverPorts){
            if(sessions.containsKey(serverPort) && !sessions.get(serverPort).isConnected())
            {
                sessions.get(serverPort).close();
                var serverAddress = new InetSocketAddress("localhost", serverPort);
                var socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(serverAddress);
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
        }
    }
    public void start() {
        try {
            this.selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            var socketAddress = new InetSocketAddress("localhost", port);
            serverSocketChannel.bind(socketAddress, port);
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            for (var serverPort : serverPorts
            ) {
                var serverAddress = new InetSocketAddress("localhost", serverPort);
                var socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(serverAddress);
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
            System.out.println("Server started...");
            while (true) {
                reconnectToServers();
                // blocking, wait for events
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    if (!key.isValid()) continue;
                    if (key.isConnectable()) connect(key);
                    else if (key.isAcceptable()) accept(key);
                    else if (key.isReadable()) {
                        var result = receivingManager.read(key);
                        if (result == null)
                            continue;
                        // object is done being transferred
                        var port = result.getRight();
                        // coming form server to client
                        if (serverPorts.contains(port)) {
                            sendingManager.sendToClient(result.getLeft(), sessions.get(receivingManager.serverIsTryingToSendTo));
                            continue;
                        }
                        // coming form client to server
                        var res = result.getLeft();
                        for (Integer serverPort : serverPorts) {
                            if (sessions.containsKey(serverPort) && sessions.get(serverPort).isConnected()) {
                                sendingManager.sendToServer(result.getLeft(), sessions.get(serverPort), port);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void connect(SelectionKey key) throws IOException {
        var socketChannel = (SocketChannel) key.channel();
        try {
            if (socketChannel.isConnectionPending()) {
                var finished = socketChannel.finishConnect();
                socketChannel.configureBlocking(false);
                socketChannel.register(this.selector, SelectionKey.OP_READ);
                this.sessions.put(socketChannel.socket().getPort(), socketChannel);
            }
        } catch (IOException ex) {
            socketChannel.close();
            var serverAddress = new InetSocketAddress("localhost", socketChannel.socket().getPort());
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(serverAddress);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void accept(SelectionKey key) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel channel = serverSocketChannel.accept();
            channel.configureBlocking(false);
            channel.register(this.selector, SelectionKey.OP_READ);
            this.sessions.put(channel.socket().getPort(), channel);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void handleRequest() {

    }
}
