import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TestClient extends WebSocketClient {
    private static volatile int globalRoomId = -1;
    private final String userId;
    private final int seat;
    private final GameAI ai;
    private static final ObjectMapper mapper = new ObjectMapper();

    public TestClient(String userId, int seat) throws Exception {
        super(new URI("ws://127.0.0.1:8181"));
        this.userId = userId;
        this.seat = seat;
        this.ai = new GameAI();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.printf("[%s] WebSocket 连接成功，座位：%d%n", userId, seat);

        if ("bot1".equals(userId)) {
            String createJson = "{\"type\":\"CREATE_ROOM\",\"data\":{\"userId\":\"bot1\",\"round\":1,\"seatNum\":0}}";
            System.out.printf("[%s] 发送创建房间: %s%n", userId, createJson);
            send(createJson);
        } else {
            new Thread(() -> {
                while (globalRoomId == -1) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                String joinJson = String.format(
                        "{\"type\":\"JOIN_ROOM\",\"data\":{\"userId\":\"%s\",\"roomId\":%d,\"seatNum\":%d}}",
                        userId, globalRoomId, seat
                );
                System.out.printf("[%s] 发送加入房间: %s%n", userId, joinJson);
                send(joinJson);
            }).start();
        }
    }

    @Override
    public void onMessage(String message) {
        System.out.printf("[%s] 收到消息：%s%n", userId, message);
        try {
            JsonNode root = mapper.readTree(message);
            String msgType = root.get("type").asText();

            if ("CREATE_ROOM".equals(msgType)) {
                int code = root.get("code").asInt();
                if (code == 200) {
                    globalRoomId = root.get("data").get("roomId").asInt();
                    int userNum = root.get("data").get("userNum").asInt();
                    System.out.printf("[%s] 创建成功，房间ID：%d，当前人数：%d%n", userId, globalRoomId, userNum);
                }
                return;
            }

            if ("JOIN_ROOM".equals(msgType)) {
                int code = root.get("code").asInt();
                if (code == 200) {
                    int userNum = root.get("data").get("userNum").asInt();
                    System.out.printf("[%s] 加入成功，当前房间人数：%d%n", userId, userNum);
                }
                return;
            }

            // ========== 调用顺序严格对应方法定义 ==========
            // 方法：handleMessage(TestClient client, String msg, int roomId, int seat)
            ai.handleMessage(this, message, globalRoomId, seat);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.printf("[%s] 连接关闭 code=%d%n", userId, code);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public static void main(String[] args) throws Exception {
        new TestClient("bot1", 0).connectBlocking();
        Thread.sleep(1000);

        new TestClient("bot2", 1).connectBlocking();
        Thread.sleep(500);

        new TestClient("bot3", 2).connectBlocking();
        Thread.sleep(500);

        new TestClient("bot4", 3).connectBlocking();

        while (true) {
            Thread.sleep(1000);
        }
    }
}