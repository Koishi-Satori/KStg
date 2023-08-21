package top.kkoishi.stg.crash;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class HandlerMain {

    static long pid;
    static String reportDir;
    private static final long PERIOD = 2500;

    @SuppressWarnings("AlibabaThreadShouldSetName")
    public static void main (String[] args) {
        assert args.length >= 2;
        pid = Long.parseLong(args[args.length - 1]);
        reportDir = args[args.length - 2];
        final var handler = new CrashHandler();
        final var threadPool = new ScheduledThreadPoolExecutor(2);
        threadPool.scheduleAtFixedRate(handler, 0L, PERIOD, TimeUnit.MILLISECONDS);
    }
}
