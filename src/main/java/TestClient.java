import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * 单AI参赛客户端
 */
public class TestClient extends WebSocketClient {
    // 全局房间ID，由服务端返回
    private static volatile int globalRoomId = -1;
    // 参赛账号、座位
    private final String userId;
    private final int seat;
    // 本地AI核心逻辑
    private final GameAI ai;
    // Jackson JSON解析器
    private static final ObjectMapper mapper = new ObjectMapper();

    // ==================== 配置区（自行修改）====================
    private static final String WS_SERVER_URL = "ws://127.0.0.1:8181";
    private static final String MY_USER_ID = "my_ai_001";
    private static final int MY_SEAT = 0;
    private static final int GAME_ROUND = 1;
    private static final int SEAT_NUM = 0;
    // ===========================================================

    // 子类构造：必须调用父类 WebSocketClient(URI)
    public TestClient(String userId, int seat) throws Exception {
        super(new URI(WS_SERVER_URL)); // 父类唯一合法构造
        this.userId = userId;
        this.seat = seat;
        this.ai = new GameAI();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.printf("[%s] WebSocket 连接服务端成功，当前座位：%d%n", userId, seat);

        // 主动创建房间
        String createJson = String.format(
                "{\"type\":\"CREATE_ROOM\",\"data\":{\"userId\":\"%s\",\"round\":%d,\"seatNum\":%d}}",
                userId, GAME_ROUND, SEAT_NUM
        );
        System.out.printf("[%s] 发送创建房间请求: %s%n", userId, createJson);
        send(createJson);
    }

    @Override
    public void onMessage(String message) {
        System.out.printf("[%s] 收到服务端消息：%s%n", userId, message);
        try {
            JsonNode root = mapper.readTree(message);
            String msgType = root.get("type").asText();

            if ("CREATE_ROOM".equals(msgType)) {
                int code = root.get("code").asInt();
                if (code == 200) {
                    globalRoomId = root.get("data").get("roomId").asInt();
                    int userNum = root.get("data").get("userNum").asInt();
                    System.out.printf("[%s] 房间创建成功 → 房间ID：%d，当前在线人数：%d%n",
                            userId, globalRoomId, userNum);
                } else {
                    System.err.printf("[%s] 创建房间失败，错误码：%d%n", userId, code);
                }
                return;
            }

            if ("JOIN_ROOM".equals(msgType)) {
                int code = root.get("code").asInt();
                if (code == 200) {
                    int userNum = root.get("data").get("userNum").asInt();
                    System.out.printf("[%s] 加入房间成功，当前房间人数：%d%n", userId, userNum);
                }
                return;
            }

            // 交给AI处理游戏消息
            ai.handleMessage(this, message, globalRoomId, seat);

        } catch (Exception e) {
            System.err.printf("[%s] 消息解析/处理异常%n", userId);
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.printf("[%s] 连接断开 → 状态码：%d，原因：%s%n", userId, code, reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.printf("[%s] 网络异常%n", userId);
        ex.printStackTrace();
    }

    /**
     * 供GameAI调用发送消息
     */
    public void sendMsg(String msg) {
        if (isOpen()) {
            send(msg);
        } else {
            System.err.println("连接已断开，无法发送消息：" + msg);
        }
    }

    // 入口：启动单个AI
    public static void main(String[] args) throws Exception {
        System.out.println("========== 本地AI 参赛客户端 启动 ==========");
        TestClient aiClient = new TestClient(MY_USER_ID, MY_SEAT);
        aiClient.connectBlocking();

        // 保活主线程
        while (true) {
            Thread.sleep(1000);
        }
    }
}