package com.orientsec.easysocket;

import com.orientsec.easysocket.exception.ReadException;
import com.orientsec.easysocket.exception.WriteException;
import com.orientsec.easysocket.inner.MessageType;
import com.orientsec.easysocket.utils.TaskAdapter;

import java.nio.ByteBuffer;

import io.reactivex.Observable;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2018/01/26 13:19
 * Author: Fredric
 * coding is art not science
 */
public class Client {

    private static class MyProtocol implements Protocol {
        @Override
        public int headSize() {
            return 12;
        }

        @Override
        public int bodySize(byte[] header) throws ReadException {
            ByteBuffer byteBuffer = ByteBuffer.wrap(header);
            return byteBuffer.getInt();
        }

        @Override
        public Message decodeMessage(byte[] header, byte[] bodyBytes) throws ReadException {
            ByteBuffer byteBuffer = ByteBuffer.wrap(header);
            Message message = new Message(0);
            message.setHeadBytes(header);
            message.setBodySize(byteBuffer.getInt());
            message.setTaskId(byteBuffer.getInt());
            message.setCmd(byteBuffer.getInt());
            message.setBodyBytes(bodyBytes);
            if (message.getCmd() == 0) {
                message.setMessageType(MessageType.PULSE);
            }
            return message;
        }

        @Override
        public byte[] encodeMessage(Message message) throws WriteException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(12);
            byteBuffer.putInt(message.getBodySize());
            byteBuffer.putInt(message.getTaskId());
            byteBuffer.putInt(message.getCmd());
            byte[] head = byteBuffer.array();
            byte[] body = message.getBodyBytes();
            byte[] sendBytes = new byte[head.length + body.length];
            System.arraycopy(head, 0, sendBytes, 0, head.length);
            System.arraycopy(body, 0, sendBytes, head.length, body.length);
            return sendBytes;
        }

        @Override
        public byte[] pulseData(Message message) {
            message.setCmd(0);
            return new byte[0];
        }
    }

    private Connection connection;

    private static class ClientHolder {
        private static Client client = new Client();
    }

    public static Client getInstance() {
        return ClientHolder.client;
    }

    private Client() {
        Options.debug = true;
        Options options = new Options.Builder()
                .connectionInfo(new ConnectionInfo("192.168.106.36", 10010))
                .protocol(new MyProtocol())
                .pulseRate(30)
                .backgroundLiveTime(60)
                .build();
        connection = EasySocket.open(options);
    }

    public Observable<String> request(String msg) {
        Task<String> task = connection.buildTask(new Request<String, String>(msg) {
            @Override
            public byte[] encode(Message message) {
                message.setCmd(1);
                return msg.getBytes();
            }

            @Override
            public void decode(Message message) {
                this.response = new String(message.getBodyBytes());
            }
        });
        return TaskAdapter.adapter(task);
    }
}
