package top.kkoishi.stg.crash;

import javax.swing.*;
import java.awt.*;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author KKoishi_
 */
public final class CrashNotice extends JFrame {

    public CrashNotice () {
        super("KStg Engine is crashed!");
        String content = "Oops, the game has crashed,\n please check the latest crash report in ";

        int exitState = -114514;
        final var f = new File(HandlerMain.reportDir + "/crash_logs/state.bin");
        if (!f.exists()) {
            System.exit(0);
        }
        try (RandomAccessFile bin = new RandomAccessFile(f, "r")) {
            exitState = bin.readInt();
            final var len = bin.readInt();
            final var bytes = new byte[len];
            if (bin.read(bytes) != -1) {
                content += new String(bytes);
            } else {
                content += HandlerMain.reportDir;
            }
        } catch (EOFException e) {
            if (exitState == -114514) {
                System.exit(0);
            }
        } catch (IOException e) {
            exitState = 1;
            content += HandlerMain.reportDir;
        }
        if (exitState != 1) {
            System.exit(0);
        }
        setSize(320, 180);
        setLocationByPlatform(true);

        final JTextArea area = new JTextArea();
        area.setFont(new Font("Arial", Font.BOLD, 10));
        area.setAutoscrolls(true);
        area.setText(content);
        add(area);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}
