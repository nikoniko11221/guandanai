import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TestClient extends WebSocketClient {

    private static final String URL = "ws://127.0.0.1:8181";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String userId;
    private final GameFrame frame;
    private final GameAI ai = new GameAI();

    private int myPos = -1;

    public TestClient(String userId, GameFrame frame) throws Exception {
        super(new URI(URL));
        this.userId = userId;
        this.frame = frame;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {

        frame.log(userId + " connected");

        try {
            if ("AI_0".equals(userId)) {

                String msg = """
                {
                    "type":"CREATE_ROOM",
                    "data":{
                        "userId":"%s",
                        "round":1
                    }
                }
                """.formatted(userId);

                send(msg);
                frame.log("AI_0 create room");

            } else {

                String msg = """
                {
                    "type":"JOIN_ROOM",
                    "data":{
                        "userId":"%s",
                        "roomId":%d
                    }
                }
                """.formatted(userId, GameRunner.roomId);

                send(msg);
                frame.log(userId + " join room");
            }

        } catch (Exception e) {
            frame.log("open error:" + e.getMessage());
        }
    }

    @Override
    public void onMessage(String message) {

        try {
            JsonNode root = MAPPER.readTree(message);

            String type = root.path("type").asText();

            if ("CREATE_ROOM".equals(type)) {
                GameRunner.roomId = root.path("data").path("roomId").asInt();
                frame.log("ROOM ID = " + GameRunner.roomId);
            }

            if ("notify".equals(type)
                    && "beginning".equals(root.path("stage").asText())) {

                myPos = root.path("myPos").asInt();
                ai.setMyPos(myPos);

                frame.log(userId + " pos=" + myPos);
            }

            if ("act".equals(type)) {
                ai.handle(this, root, frame, myPos, GameRunner.roomId);
            }

        } catch (Exception e) {
            frame.log("parse error:" + e.getMessage());
        }
    }

    public void sendMsg(String json) {
        if (isOpen()) send(json);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        frame.log(userId + " closed");
    }

    @Override
    public void onError(Exception ex) {
        frame.log(userId + " error:" + ex.getMessage());
    }
}