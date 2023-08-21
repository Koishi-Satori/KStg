package top.kkoishi.stg.crash;

import static top.kkoishi.stg.crash.HandlerMain.pid;

/**
 * @author KKoishi_
 */
public final class CrashHandler implements Runnable {
    private final Object lock = new Object();
    private boolean shouldEnd = false;

    private static void handleCrash () {
        new CrashNotice().setVisible(true);
    }

    @Override
    public void run () {
        synchronized (lock) {
            if (shouldEnd) {
                return;
            }
        }
        final var optional = ProcessHandle.of(pid);
        if (optional.isEmpty() || !optional.get().isAlive()) {
            System.err.println("No process found, ready to handle crash.");
            synchronized (lock) {
                handleCrash();
                shouldEnd = true;
            }
        } else {
            System.out.println("Find process!");
        }
    }


    public boolean isShouldEnd () {
        synchronized (lock) {
            return shouldEnd;
        }
    }
}
