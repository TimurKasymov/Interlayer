package network;

import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class SendingManager {

    private final int PACKET_SIZE = 1024;
    private final int DATA_SIZE = PACKET_SIZE - 1;


    public void sendToClient(byte[] data, SocketChannel socketChannel) {
        byte[][] ret = new byte[(int) Math.ceil(data.length / (double) DATA_SIZE)][DATA_SIZE];
        try {
            int start = 0;
            for (int i = 0; i < ret.length; i++) {
                ret[i] = Arrays.copyOfRange(data, start, start + DATA_SIZE);
                start += DATA_SIZE;
            }
            for (int i = 0; i < ret.length; i++) {
                var chunk = ret[i];
                if (i == ret.length - 1) {
                    var lastChunk = Bytes.concat(chunk, new byte[]{1});
                    socketChannel.write(ByteBuffer.wrap(lastChunk));
                    System.out.println("Последний чанк размером " + chunk.length + " отправлен на сервер.");
                } else {
                    socketChannel.write(ByteBuffer.wrap(Bytes.concat(chunk, new byte[]{0})));
                    System.out.println("Чанк размером " + chunk.length + " отправлен на сервер.");
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            // server has crashed, and we got isConnectionPending - true and isOpen - true as well
            // and so we close the connection, because this connection is already closed by the server and create another one
            if (Objects.equals(e.getMessage(), "Connection reset")) {
                try {
                    Thread.sleep(3000);
                    socketChannel.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void sendToServer(byte[] data, SocketChannel socketChannel, Integer from) {

        try {
            var st = from.toString().split("");
            var bytes = Arrays.stream(st).map(Byte::valueOf).toArray();
            var cutPointer = 1024;
            for(int i = data.length-1; i>-1 && data[i] == 0; i--){
                cutPointer = i;
            }
            data = Arrays.copyOfRange(data, 0, cutPointer);
            byte[][] ret = new byte[(int) Math.ceil(data.length / (double) (DATA_SIZE - st.length))][DATA_SIZE];
            int start = 0;
            for (int i = 0; i < ret.length; i++) {
                ret[i] = Arrays.copyOfRange(data, start, start + DATA_SIZE - st.length);
                for (Object aByte : bytes) {
                    var b = new byte[]{(Byte) aByte};
                    ret[i] = Bytes.concat(ret[i], b);
                }
                start += DATA_SIZE - st.length;
            }

            System.out.println("Отправляется " + ret.length + " чанков...");

            for (int i = 0; i < ret.length; i++) {
                var chunk = ret[i];
                if (i == ret.length - 1) {
                    var lastChunk = Bytes.concat(chunk, new byte[]{1});
                    socketChannel.write(ByteBuffer.wrap(lastChunk));
                    System.out.println("Последний чанк размером " + chunk.length + " отправлен на сервер.");
                } else {
                    socketChannel.write(ByteBuffer.wrap(Bytes.concat(chunk, new byte[]{0})));
                    System.out.println("Чанк размером " + chunk.length + " отправлен на сервер.");
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            // server has crashed, and we got isConnectionPending - true and isOpen - true as well
            // and so we close the connection, because this connection is already closed by the server and create another one
            if (Objects.equals(e.getMessage(), "Connection reset")) {
                try {
                    Thread.sleep(3000);
                    socketChannel.close();
                } catch (Exception e1) {
                }
            }
        }
    }
}
