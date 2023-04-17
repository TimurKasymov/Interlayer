package network;

import com.google.common.primitives.Bytes;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ReceivingManager {

    private HashSet<SocketChannel> sessions = new HashSet<>();
    private final HashMap<Integer, byte[]> receivedDataFromClients;
    private final HashMap<Integer, byte[]> receivedDataFromServers;
    private List<Integer> serverPorts;
    private List<Integer> clientPorts;
    public Integer serverIsTryingToSendTo;

    public ReceivingManager(List<Integer> serverPorts, List<Integer> clientPorts){
        this.serverPorts = serverPorts;
        this.clientPorts = clientPorts;
        this.receivedDataFromClients = new HashMap<>();
        this.receivedDataFromServers = new HashMap<>();
    }

    public void setSessions(HashSet<SocketChannel> sessions){
        this.sessions = sessions;
    }
    public Pair<byte[], Integer> read(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int numRead = channel.read(byteBuffer);
            if (numRead == -1) {
                this.sessions.remove(channel);
                System.out.println("client " + channel.socket().getRemoteSocketAddress() + " disconnected");
                channel.close();
                key.cancel();
                return null;
            }
            var portSentFrom = channel.socket().getPort();
            //being sent from client
            if(clientPorts.contains(portSentFrom)){
                if(!receivedDataFromClients.containsKey(portSentFrom)){
                    receivedDataFromClients.put(portSentFrom, Arrays.copyOf(byteBuffer.array(), byteBuffer.array().length - 1));
                }
                else{
                    var arr = receivedDataFromClients.get(portSentFrom);
                    arr = Bytes.concat(arr, Arrays.copyOf(byteBuffer.array(), byteBuffer.array().length - 1));
                }
                // reached the end of the object being sent
                if(byteBuffer.array()[numRead-1] == 1){
                    var pair = ImmutablePair.of(receivedDataFromClients.get(portSentFrom), portSentFrom);
                    receivedDataFromClients.remove(portSentFrom);
                    return pair;
                }
            }
            else {
                //being sent from server
                var strB = new StringBuilder();
                for(int i = 0; i < 5; i++){
                    strB.append(((Byte) byteBuffer.array()[numRead-2-i]));
                }
                serverIsTryingToSendTo = Integer.parseInt(strB.reverse().toString());
                if(!receivedDataFromServers.containsKey(serverIsTryingToSendTo)){
                    receivedDataFromServers.put(serverIsTryingToSendTo, Arrays.copyOf(byteBuffer.array(), byteBuffer.array().length - 6));
                }
                else{
                    var arr = receivedDataFromServers.get(serverIsTryingToSendTo);
                    receivedDataFromServers.put(serverIsTryingToSendTo, Bytes.concat(arr, Arrays.copyOf(byteBuffer.array(), byteBuffer.array().length - 6)));
                }
                // reached the end of the object being sent
                if(byteBuffer.array()[numRead-1] == 1){
                    var pair = ImmutablePair.of(receivedDataFromServers.get(serverIsTryingToSendTo), portSentFrom);
                    receivedDataFromServers.remove(serverIsTryingToSendTo);
                    return pair;
                }
            }
        }
        catch (IOException e){
            System.err.println(e.getMessage());
            // server has crashed, and we got isConnectionPending - true and isOpen - true as well
            // and so we close the connection, because this connection is already closed by the server and create another one
            if(Objects.equals(e.getMessage(), "Connection reset"))
            {
                try{
                    Thread.sleep(3000);
                    channel.close();
                }
                catch (Exception e1){
                }
            }
        }

        return null;
    }
}
