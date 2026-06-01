import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TestClient extends WebSocketClient {

    public static final String WS_SERVER_URL = "ws://127.0.0.1:8181";
    public static final String MY_USER_ID = "my_ai_001";
    public static final int GAME_ROUND = 1;
    public static final int SEAT_NUM = 0;

    private static final ObjectMapper mapper = new ObjectMapper();

    private final String userId;
    private final int seat;
    private final GameAI ai;
    private final MainFrame frame;

    private int roomId = -1;

    public TestClient(String userId, int seat, MainFrame frame) throws Exception {
        super(new URI(WS_SERVER_URL));
        this.userId = userId;
        this.seat = seat;
        this.frame = frame;
        this.ai = new GameAI();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        frame.appendLog("连接成功：" + userId);

        String createJson = String.format(
                "{\"type\":\"CREATE_ROOM\",\"data\":{\"userId\":\"%s\",\"round\":%d,\"seatNum\":%d}}",
                userId, GAME_ROUND, SEAT_NUM
        );

        send(createJson);
    }

    @Override
    public void onMessage(String message) {
        frame.appendLog(message);

        try {
            JsonNode root = mapper.readTree(message);
            String type = root.path("type").asText("");

            // ========= CREATE ROOM =========
            if ("CREATE_ROOM".equals(type)) {
                if (root.path("code").asInt() == 200) {
                    roomId = root.path("data").path("roomId").asInt(-1);
                    frame.appendLog("房间创建成功：" + roomId);
                }
                return;
            }

            // ========= JOIN ROOM =========
            if ("JOIN_ROOM".equals(type)) {
                if (root.path("code").asInt() == 200) {
                    frame.appendLog("加入房间成功");
                }
                return;
            }

            // ========= INIT（只做UI刷新） =========
            if ("init".equals(type)) {
                JsonNode players = root.path("data").path("playerInfo");
                if (players.isArray()) {
                    frame.refreshAllPlayer(players);
                }
                return;
            }

            // ========= 只把对局决策交给AI =========
            if ("act".equals(type)) {
                ai.handleMessage(this, message, roomId, seat, frame);
            }

        } catch (Exception e) {
            frame.appendLog("解析异常");
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        frame.appendLog("断开连接：" + reason);
    }

    @Override
    public void onError(Exception ex) {
        frame.appendLog("网络错误");
        ex.printStackTrace();
    }

    public void sendMsg(String msg) {
        if (isOpen()) send(msg);
    }
}