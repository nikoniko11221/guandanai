public class GameRunner extends Thread {
    private final MainFrame frame;
    private TestClient myAi;
    private volatile boolean running = true;

    public GameRunner(MainFrame frame) {
        this.frame = frame;
    }

    @Override
    public void run() {
        try {
            frame.appendLog("========== 单个AI 启动 ==========");
            frame.appendLog("服务地址：" + TestClient.WS_SERVER_URL);

            myAi = new TestClient("my_guandan_ai", 0, frame);
            frame.appendLog("AI 正在连接服务端...");
            myAi.connectBlocking();

            frame.appendLog("AI 已就绪，可手动加入指定房间");

            while (running && !isInterrupted()) {
                Thread.sleep(500);
            }
        } catch (Exception e) {
            frame.appendLog("AI 运行异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
            frame.appendLog("AI 已断开连接");
        }
    }

    public void joinTargetRoom(int roomId) {
        if (myAi != null) {
            myAi.joinRoom(roomId);
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (myAi != null) {
                myAi.close();
            }
        } catch (Exception ignored) {
        }
    }

    public TestClient getAiClient() {
        return myAi;
    }
}