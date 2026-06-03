import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    private final JTextArea log = new JTextArea();

    public GameFrame() {
        setTitle("OpenGuanDan AI");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        log.setEditable(false);
        add(new JScrollPane(log), BorderLayout.CENTER);

        setVisible(true);
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getText().length());
        });
    }
}