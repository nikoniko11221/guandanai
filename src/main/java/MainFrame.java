import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;

/**
 * 掼蛋 AI 对战平台（美化版 + 无乱码稳定版）
 */
public class MainFrame extends JFrame {

    private final JLabel[] playerLabels = new JLabel[4];
    private final JTextArea logArea;
    private final JLabel titleLabel;

    public MainFrame() {

        setTitle("掼蛋AI对战平台");
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // =========================
        // ⭐ 字体统一（防乱码核心）
        // =========================
        Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 24);
        Font normalFont = new Font("Microsoft YaHei", Font.PLAIN, 14);

        // =========================
        // 顶部标题（带花色）
        // =========================
        titleLabel = new JLabel("♠ 掼蛋 AI 对战平台 ♥ ♦ ♣", SwingConstants.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(30, 30, 30));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(titleLabel, BorderLayout.NORTH);

        // =========================
        // 中间玩家区域（牌桌风格）
        // =========================
        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        for (int i = 0; i < 4; i++) {

            JLabel label = new JLabel("等待玩家 " + i, SwingConstants.CENTER);
            label.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));

            label.setOpaque(true);
            label.setBackground(new Color(245, 245, 245));
            label.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            playerLabels[i] = label;
            centerPanel.add(label);
        }

        add(centerPanel, BorderLayout.CENTER);

        // =========================
        // 日志区（游戏控制台风）
        // =========================
        logArea = new JTextArea();
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setEditable(false);

        logArea.setBackground(new Color(20, 20, 20));
        logArea.setForeground(new Color(0, 255, 120));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("对局日志"));

        scrollPane.setPreferredSize(new Dimension(1100, 260));

        add(scrollPane, BorderLayout.SOUTH);
    }

    // =========================
    // 日志输出
    // =========================
    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("▶ " + text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // =========================
    // 更新玩家状态
    // =========================
    public void updatePlayerState(int index, String state) {
        SwingUtilities.invokeLater(() -> {

            for (int i = 0; i < 4; i++) {
                playerLabels[i].setBackground(new Color(245, 245, 245));
            }

            if (index >= 0 && index < 4) {
                playerLabels[index].setBackground(new Color(255, 235, 150));
                playerLabels[index].setText("玩家 " + index + " ｜ " + state);
            }
        });
    }

    // =========================
    // 初始化玩家信息
    // =========================
    public void refreshAllPlayer(JsonNode playerArray) {

        SwingUtilities.invokeLater(() -> {

            for (int i = 0; i < 4; i++) {
                playerLabels[i].setText("玩家 " + i + " ｜ 等待对局");
            }

            for (JsonNode node : playerArray) {

                int idx = node.path("idx").asInt(0);
                String userId = node.path("userId").asText("未知");
                int cards = node.path("cards").asInt(0);

                if (idx < 0 || idx >= 4) continue;

                playerLabels[idx].setText(
                        "<html><b>座位 " + idx + "</b><br/>"
                                + "玩家：" + userId + "<br/>"
                                + "剩余牌：" + cards + "</html>"
                );
            }
        });
    }

    // =========================
    // 测试入口
    // =========================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}