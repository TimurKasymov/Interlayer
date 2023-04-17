import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Bytes;
import manager.TransferManager;
import network.ReceivingManager;
import network.SendingManager;
import network.TCPServer;
import settings.SettingsModel;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            var inputStream = classloader.getResourceAsStream("settings.json");
            if (inputStream == null){
                System.out.println("settings file not found");
                System.exit(-1);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            var str = sb.toString();
            ObjectMapper mapper = new ObjectMapper();
            var settings = mapper.readValue(str, SettingsModel.class);
            var sendManager = new SendingManager();
            var receiveManager = new ReceivingManager(settings.serverPorts, settings.clientPorts);
            var tcpServer = new TCPServer(settings.serverPorts, settings.clientPorts, settings.localPort, sendManager, receiveManager);
            var transferManager = new TransferManager(receiveManager, sendManager, tcpServer);
            transferManager.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}