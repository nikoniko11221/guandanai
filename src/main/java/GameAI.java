import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class GameAI {
    private static final ObjectMapper mapper = new ObjectMapper();

    public void handleMessage(TestClient client, String msg, int roomId, int seat, MainFrame frame) {
        try {
            JsonNode root = mapper.readTree(msg);
            String type = root.path("type").asText("");

            if ("notify".equals(type)) {
                frame.appendLog("【对局广播】" + root);
                return;
            }

            if (!"act".equals(type)) return;

            String stage = root.path("stage").asText("");
            JsonNode actionList = root.path("actionList");
            if (actionList == null || !actionList.isArray() || actionList.size() == 0) return;

            int myPos = root.path("myPos").asInt(0);
            JsonNode best = chooseBestAction(actionList, root, stage);
            String actStr = best.toString();
            String sendJson;

            switch (stage) {
                case "tribute":
                    sendJson = String.format(
                            "{\"type\":\"TRIBUTE\",\"data\":{\"roomId\":%d,\"player\":%d,\"act\":%s}}",
                            roomId, myPos, actStr
                    );
                    break;
                case "back":
                    sendJson = String.format(
                            "{\"type\":\"PAYTRIBUTE\",\"data\":{\"roomId\":%d,\"player\":%d,\"act\":%s}}",
                            roomId, myPos, actStr
                    );
                    break;
                default:
                    sendJson = String.format(
                            "{\"type\":\"PLAY\",\"data\":{\"roomId\":%d,\"player\":%d,\"act\":%s}}",
                            roomId, myPos, actStr
                    );
            }
            client.sendMsg(sendJson);
        } catch (Exception e) {
            frame.appendLog("AI 处理出牌异常");
            e.printStackTrace();
        }
    }

    private JsonNode chooseBestAction(JsonNode actionList, JsonNode root, String stage) {
        List<JsonNode> passList = new ArrayList<>();
        List<JsonNode> bombList = new ArrayList<>();
        List<JsonNode> singleList = new ArrayList<>();
        List<JsonNode> otherList = new ArrayList<>();
        JsonNode greaterAction = root.path("greaterAction");

        for (JsonNode act : actionList) {
            String type = act.get(0).asText("");
            if ("PASS".equals(type)) {
                passList.add(act);
            } else if (type.contains("Bomb")) {
                bombList.add(act);
            } else if (type.contains("Single")) {
                singleList.add(act);
            } else {
                otherList.add(act);
            }
        }

        if ("tribute".equals(stage) || "back".equals(stage)) {
            return actionList.get(0);
        }

        if (actionList.size() == 1 && passList.size() == 1) {
            return passList.get(0);
        }

        for (JsonNode act : actionList) {
            String type = act.get(0).asText("");
            if (type.contains("Bomb") && actionList.size() > 3) continue;
            if (!"PASS".equals(type) && !type.contains("Bomb")) {
                return act;
            }
        }

        if (!passList.isEmpty()) {
            return passList.get(0);
        }
        return actionList.get(0);
    }
}