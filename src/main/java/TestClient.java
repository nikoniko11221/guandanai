import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TestClient extends WebSocketClient {
    public static final String WS_SERVER_URL = "ws://127.0.0.1:8181";
    public static final int GAME_ROUND = 1;

    private static final ObjectMapper mapper = new ObjectMapper();
    private final String userId;
    private final int seat;
    private final GameAI ai;
    private final MainFrame frame;

    public int roomId = -1;

    public TestClient(String userId, int seat, MainFrame frame) throws Exception {
        super(new URI(WS_SERVER_URL));
        this.userId = userId;
        this.seat = seat;
        this.frame = frame;
        this.ai = new GameAI();
    }

    // 公开发送消息方法，确保外部可调用
    public void sendMsg(String msg) {
        if (isOpen()) {
            send(msg);
        }
    }

    public void joinRoom(int targetRoomId) {
        if (!isOpen()) {
            frame.appendLog("连接未就绪，无法加入房间");
            return;
        }
        String json = String.format(
                "{\"type\":\"JOIN_ROOM\",\"data\":{\"userId\":\"%s\",\"roomId\":%d}}",
                this.userId, targetRoomId
        );
        send(json);
        frame.appendLog("请求加入房间：" + targetRoomId);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        frame.appendLog("[" + userId + "] WebSocket 连接成功");
        // 默认自动创建房间
        String createJson = String.format(
                "{\"type\":\"CREATE_ROOM\",\"data\":{\"userId\":\"%s\",\"round\":%d,\"seatNum\":%d}}",
                userId, GAME_ROUND, seat
        );
        send(createJson);
    }

    @Override
    public void onMessage(String message) {
        frame.appendLog("收到消息：" + message);

        new Thread(() -> {
            try {
                JsonNode root = mapper.readTree(message);
                String type = root.path("type").asText("");

                if ("CREATE_ROOM".equals(type)) {
                    if (root.path("code").asInt() == 200) {
                        roomId = root.path("data").path("roomId").asInt(-1);
                        frame.appendLog("[" + userId + "] 创建房间成功 → 房间号：" + roomId);
                    } else {
                        frame.appendLog("[" + userId + "] 创建房间失败");
                    }
                    return;
                }

                if ("JOIN_ROOM".equals(type)) {
                    if (root.path("code").asInt() == 200) {
                        roomId = root.path("data").path("roomId").asInt(-1);
                        frame.appendLog("[" + userId + "] 成功进入房间：" + roomId);
                    } else {
                        frame.appendLog("[" + userId + "] 加入房间失败");
                    }
                    return;
                }

                if ("init".equals(type)) {
                    JsonNode players = root.path("data").path("playerInfo");
                    if (players.isArray()) {
                        frame.refreshAllPlayer(players);
                    }
                }

                if ("act".equals(type)) {
                    ai.handleMessage(this, message, roomId, seat, frame);
                }
            } catch (Exception e) {
                frame.appendLog("消息解析异常");
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        frame.appendLog("[" + userId + "] 连接断开：" + reason);
    }

    @Override
    public void onError(Exception ex) {
        frame.appendLog("[" + userId + "] 网络异常");
        ex.printStackTrace();
    }
}