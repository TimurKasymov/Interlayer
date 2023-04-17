package manager;

import network.ReceivingManager;
import network.SendingManager;
import network.TCPServer;

public class TransferManager {

    private ReceivingManager receivingManager;
    private SendingManager sendingManager;
    private TCPServer server;

    public TransferManager(ReceivingManager receivingManager, SendingManager sendingManager, TCPServer server) {
        this.receivingManager = receivingManager;
        this.sendingManager = sendingManager;
        this.server = server;
    }

    public void start(){
        server.start();
    }

}
