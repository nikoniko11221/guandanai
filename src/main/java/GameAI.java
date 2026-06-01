import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class GameAI {
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> hand = new ArrayList<>();

    // 固定参数顺序：客户端对象、消息字符串、房间ID、座位号
    public void handleMessage(TestClient client, String msg, int roomId, int seat) {
        try {
            JsonNode root = mapper.readTree(msg);
            if (root == null || !root.has("type")) return;

            String type = root.get("type").asText();

            // ================= 1. 处理通知类消息 (notify) =================
            if ("notify".equals(type)) {
                if (!root.has("stage")) return;
                String stage = root.get("stage").asText();

                // 开局发牌
                if ("beginning".equals(stage) && root.has("handCards")) {
                    hand.clear();
                    for (JsonNode c : root.get("handCards")) {
                        hand.add(c.asText());
                    }
                    System.out.println("===== 游戏正式开始，座位 " + seat + " 手牌：" + hand + " =====");
                }
                return;
            }

            // ================= 2. 处理决策类消息 (act) =================
            if ("act".equals(type)) {
                if (!root.has("actionList") || !root.has("stage")) return;

                String stage = root.get("stage").asText();
                JsonNode actionList = root.get("actionList");

                if (actionList == null || actionList.isEmpty()) {
                    System.out.printf("[警告] 座位 %d 收到 act 消息，但 actionList 为空。%n", seat);
                    return;
                }

                // --- 基础策略：挑选最佳动作 ---
                JsonNode bestAct = null;
                for (JsonNode act : actionList) {
                    String actionType = act.get(0).asText();
                    // 积极出牌，优先选择非“不要(PASS)”的动作
                    if (!"PASS".equalsIgnoreCase(actionType)) {
                        bestAct = act;
                        break;
                    }
                }
                if (bestAct == null) {
                    bestAct = actionList.get(0);
                }

                // --- 根据官方文档规范，严格分支组装响应报文 ---
                String responseJson = "";

                if ("play".equals(stage)) {
                    // 3.3 正常打牌请求
                    responseJson = String.format(
                            "{\"type\":\"PLAY\",\"data\":{\"roomId\":%d,\"player\":%d,\"act\":%s}}",
                            roomId, seat, bestAct.toString()
                    );
                }
                else if ("tribute".equals(stage)) {
                    // 3.4 进贡请求
                    responseJson = String.format(
                            "{\"type\":\"TRIBUTE\",\"data\":{\"roomId\":%d,\"player\":%d,\"act\":%s}}",
                            roomId, seat, bestAct.toString()
                    );
                }
                else if ("back".equals(stage)) {
                    // 3.5 还牌请求 (PAYTRIBUTE) - 需要透传 tributePos 和 tribute
                    int tributePos = root.get("tributePos").asInt();
                    String tributeCard = root.get("tribute").asText();

                    responseJson = String.format(
                            "{\"type\":\"PAYTRIBUTE\",\"data\":{\"roomId\":%d,\"player\":%d,\"tributePos\":%d,\"tribute\":\"%s\",\"act\":%s}}",
                            roomId, seat, tributePos, tributeCard, bestAct.toString()
                    );
                }
                else {
                    // 兜底逻辑：如果有未知的 act 阶段，尝试转大写提交，防患于未然
                    responseJson = String.format(
                            "{\"type\":\"%s\",\"data\":{\"roomId\":%d,\"player\":%d,\"act\":%s}}",
                            stage.toUpperCase(), roomId, seat, bestAct.toString()
                    );
                }

                System.out.printf("[AI 决策成功] 座位 %d 在 [%s] 阶段做出动作: %s%n", seat, stage, responseJson);

                // 发送给服务器
                client.send(responseJson);
            }

        } catch (Exception e) {
            System.err.println("AI 处理消息时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}