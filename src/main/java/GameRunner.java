/**
 * 独立子线程：启动多个AI客户端，用于对战实验
 * ✔ 支持3个AI同时上线
 * ✔ 避免静态字段依赖
 * ✔ 可稳定运行 WebSocket 对局
 */
public class GameRunner extends Thread {

    private final MainFrame frame;

    private TestClient ai1;
    private TestClient ai2;
    private TestClient ai3;

    private volatile boolean running = true;

    public GameRunner(MainFrame frame) {
        this.frame = frame;
    }

    @Override
    public void run() {
        try {
            frame.appendLog("========== AI对战系统启动 ==========");

            String serverUrl = TestClient.WS_SERVER_URL;

            frame.appendLog("连接服务器：" + serverUrl);

            // =========================
            // 1️⃣ 创建3个AI客户端
            // =========================
            ai1 = new TestClient("python_ai_1", 1, frame);
            ai2 = new TestClient("python_ai_2", 2, frame);
            ai3 = new TestClient("python_ai_3", 3, frame);

            // =========================
            // 2️⃣ 依次连接（避免并发冲突）
            // =========================
            frame.appendLog("AI1连接中...");
            ai1.connectBlocking();

            frame.appendLog("AI2连接中...");
            ai2.connectBlocking();

            frame.appendLog("AI3连接中...");
            ai3.connectBlocking();

            frame.appendLog("========== 所有AI已连接，等待对局开始 ==========");

            // =========================
            // 3️⃣ 保活线程（防止退出）
            // =========================
            while (running && !isInterrupted()) {
                Thread.sleep(500);
            }

        } catch (Exception e) {
            frame.appendLog("GameRunner异常：" + e.getMessage());
            e.printStackTrace();

        } finally {
            shutdown();
            frame.appendLog("========== AI对战系统已退出 ==========");
        }
    }

    /**
     * 外部安全停止
     */
    public void shutdown() {
        running = false;

        try {
            if (ai1 != null) ai1.close();
            if (ai2 != null) ai2.close();
            if (ai3 != null) ai3.close();
        } catch (Exception ignored) {
        }
    }
}