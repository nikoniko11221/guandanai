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

            // =========================
            // ⭐ 核心：策略选择
            // =========================
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
            frame.appendLog("AI 处理异常");
            e.printStackTrace();
        }
    }

    /**
     * ⭐ 核心策略函数
     */
    private JsonNode chooseBestAction(JsonNode actionList, JsonNode root, String stage) {

        List<JsonNode> passList = new ArrayList<>();
        List<JsonNode> bombList = new ArrayList<>();
        List<JsonNode> singleList = new ArrayList<>();
        List<JsonNode> otherList = new ArrayList<>();

        // 当前最大牌
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

        // =========================
        // ⭐ 1. 进贡 / 还礼阶段
        // =========================
        if ("tribute".equals(stage) || "back".equals(stage)) {
            // 优先最小牌（通常在列表前面更小）
            return actionList.get(0);
        }

        // =========================
        // ⭐ 2. PLAY阶段核心策略
        // =========================

        // ❗情况1：只能PASS
        if (actionList.size() == 1 && passList.size() == 1) {
            return passList.get(0);
        }

        // ❗情况2：有压制能力（优先非PASS）
        for (JsonNode act : actionList) {
            String type = act.get(0).asText("");

            // 避免乱炸（除非快赢）
            if (type.contains("Bomb") && actionList.size() > 3) continue;

            // 优先小牌消耗（更容易赢）
            if (!"PASS".equals(type) && !type.contains("Bomb")) {
                return act;
            }
        }

        // ❗情况3：必须PASS时避免乱送
        if (!passList.isEmpty()) {
            return passList.get(0);
        }

        // ❗兜底
        return actionList.get(0);
    }
}