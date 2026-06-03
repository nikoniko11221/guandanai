import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class GameAI {

    private int myPos = -1;

    public void setMyPos(int pos) {
        this.myPos = pos;
    }

    public void handle(TestClient client,
                       JsonNode root,
                       GameFrame frame,
                       int myPos,
                       int roomId) {

        try {
            String stage = root.path("stage").asText();
            JsonNode actionList = root.path("actionList");

            if (!actionList.isArray() || actionList.size() == 0) return;

            int index = chooseIndex(actionList, stage);

            JsonNode act = actionList.get(index);

            String json;

            switch (stage) {

                case "tribute" -> json = """
                {
                    "type":"TRIBUTE",
                    "data":{
                        "roomId":%d,
                        "player":%d,
                        "act":%s
                    }
                }
                """.formatted(roomId, myPos, act);

                case "back" -> {
                    int triPos = root.path("tributePos").asInt();
                    String triCard = root.path("tribute").asText();

                    json = """
                    {
                        "type":"PAYTRIBUTE",
                        "data":{
                            "roomId":%d,
                            "player":%d,
                            "tributePos":%d,
                            "tribute":"%s",
                            "act":%s
                        }
                    }
                    """.formatted(roomId, myPos, triPos, triCard, act);
                }

                default -> json = """
                {
                    "type":"PLAY",
                    "data":{
                        "roomId":%d,
                        "player":%d,
                        "act":%s
                    }
                }
                """.formatted(roomId, myPos, act);
            }

            client.sendMsg(json);

            frame.log("AI[" + myPos + "] " + stage + " -> " + act);

        } catch (Exception e) {
            frame.log("AI error:" + e.getMessage());
        }
    }

    // =========================
    // 🎯 核心：选择策略（随机 + 稳定）
    // =========================
    private int chooseIndex(JsonNode actionList, String stage) {

        List<Integer> candidates = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();

        int bestScore = Integer.MIN_VALUE;

        // ① 计算所有动作分数
        for (int i = 0; i < actionList.size(); i++) {
            JsonNode act = actionList.get(i);
            int s = score(act, stage);

            scores.add(s);
            bestScore = Math.max(bestScore, s);
        }

        // ② 保留“接近最优”的动作（人类行为核心）
        int threshold = bestScore - 1200;

        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i) >= threshold) {
                candidates.add(i);
            }
        }

        // ③ 防止极端情况（全PASS等）
        if (candidates.isEmpty()) {
            for (int i = 0; i < actionList.size(); i++) {
                candidates.add(i);
            }
        }

        // ④ 随机选一个（人类风格）
        return candidates.get((int) (Math.random() * candidates.size()));
    }

    private int score(JsonNode act, String stage) {

        String type = act.get(0).asText();

        // ❌ PASS：低优先级但不是绝对禁止
        if ("PASS".equals(type)) {
            return -4000 + (int)(Math.random() * 500);
        }

        int base = switch (type) {
            case "Bomb" -> 5000;
            case "StraightFlush" -> 4200;
            case "Straight" -> 3200;
            case "ThreeWithTwo" -> 2600;
            case "TwoTrips" -> 2400;
            case "ThreePair" -> 2200;
            case "Trips" -> 2000;
            case "Pair" -> 1200;
            case "Single" -> 800;
            default -> 500;
        };

        int size = act.get(2).size();

        // 👉 人类行为：偶尔不打最大
        int randomness = (int) (Math.random() * 400);

        // 👉 控制节奏：避免一次出太多（防止滚雪球）
        int balance = (size >= 5) ? -300 : 0;

        // 👉 出牌奖励（但不极端）
        int sizeBonus = size * 600;

        // 👉 轻微惩罚炸弹（避免乱炸）
        int bombPenalty = "Bomb".equals(type) ? -200 : 0;

        return base + sizeBonus + randomness + balance + bombPenalty;
    }
}