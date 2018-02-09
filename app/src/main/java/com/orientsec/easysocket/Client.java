package com.orientsec.easysocket;

import com.orientsec.easysocket.adapter.TaskAdapter;
import com.orientsec.easysocket.exception.ReadException;
import com.orientsec.easysocket.exception.WriteException;
import com.orientsec.easysocket.inner.MessageType;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;

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
        private int id;

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
            Message message;
            byteBuffer.getInt();
            int taskId = byteBuffer.getInt();
            int cmd = byteBuffer.getInt();
            if (cmd == 0) {
                message = new Message(MessageType.PULSE);
            } else if (cmd == 1) {
                message = new Message(MessageType.AUTH);
            } else {
                message = new Message(MessageType.REQUEST);
            }
            message.setCmd(cmd);
            message.setTaskId(taskId);
            message.setBody(bodyBytes);
            return message;
        }

        @Override
        public byte[] encodeMessage(Message message) throws WriteException {
            byte[] body = (byte[]) message.getBody();
            if (body == null) {
                body = new byte[0];
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(16);
            byteBuffer.putInt(body.length);
            byteBuffer.putInt(message.getTaskId());
            if (message.getMessageType() == MessageType.PULSE) {
                byteBuffer.putInt(0);
            } else if (message.getMessageType() == MessageType.AUTH) {
                byteBuffer.putInt(1);
            } else {
                byteBuffer.putInt(2);
            }
            byteBuffer.putInt(id);
            byte[] head = byteBuffer.array();
            byte[] sendBytes = new byte[head.length + body.length];
            System.arraycopy(head, 0, sendBytes, 0, head.length);
            System.arraycopy(body, 0, sendBytes, head.length, body.length);
            return sendBytes;
        }

        @Override
        public boolean authorize(Object data) {
            id = ByteBuffer.wrap((byte[]) data).getInt();
            return true;
        }

        @Override
        public boolean needAuthorize() {
            return true;
        }

        @Override
        public SSLContext sslContext() throws Exception {
            return null;
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
            public Object encode() {
                return msg.getBytes();
            }

            @Override
            public String decode(Object data) {
                return new String((byte[]) data);
            }
        });
        return TaskAdapter.toObservable(task);
    }
}
