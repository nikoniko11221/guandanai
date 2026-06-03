public class GameRunner {

    public static volatile int roomId = -1;

    public static void main(String[] args) throws Exception {

        GameFrame frame = new GameFrame();

        TestClient ai0 = new TestClient("AI_0", frame);
        ai0.connectBlocking();

        while (roomId == -1) {
            Thread.sleep(100);
        }

        frame.log("ROOM CREATED: " + roomId);

        Thread.sleep(500);

        for (int i = 1; i <= 3; i++) {
            TestClient ai = new TestClient("AI_" + i, frame);
            ai.connectBlocking();
            Thread.sleep(200);
        }

        frame.log("ALL AI CONNECTED");
    }
}