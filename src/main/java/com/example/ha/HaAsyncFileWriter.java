package com.example.ha;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

final class HaAsyncFileWriter {
    private static final Object LOCK = new Object();
    private static final Map<String, WriteOperation> PENDING_WRITES = new HashMap<String, WriteOperation>();
    private static final Set<String> ACTIVE_KEYS = new HashSet<String>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "HashimotoAddons-Persistence");
            thread.setDaemon(true);
            return thread;
        }
    });

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                flush(3000L);
            }
        }, "HashimotoAddons-Persistence-Shutdown"));
    }

    private HaAsyncFileWriter() {
    }

    static void submit(Path path, WriteOperation operation) {
        if (path == null || operation == null) {
            return;
        }

        String key = path.toAbsolutePath().normalize().toString();
        boolean startWorker;
        synchronized (LOCK) {
            PENDING_WRITES.put(key, operation);
            startWorker = ACTIVE_KEYS.add(key);
        }
        if (startWorker) {
            EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    drain(key);
                }
            });
        }
    }

    static boolean flush(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + Math.max(1L, timeoutMillis);
        synchronized (LOCK) {
            while (!PENDING_WRITES.isEmpty() || !ACTIVE_KEYS.isEmpty()) {
                long remainingMillis = deadline - System.currentTimeMillis();
                if (remainingMillis <= 0L) {
                    System.err.println("[HashimotoAddons] Timed out while flushing persistence writes.");
                    return false;
                }
                try {
                    LOCK.wait(remainingMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    System.err.println("[HashimotoAddons] Interrupted while flushing persistence writes.");
                    return false;
                }
            }
        }
        return true;
    }

    private static void drain(String key) {
        while (true) {
            WriteOperation operation;
            synchronized (LOCK) {
                operation = PENDING_WRITES.remove(key);
                if (operation == null) {
                    ACTIVE_KEYS.remove(key);
                    LOCK.notifyAll();
                    return;
                }
            }

            try {
                operation.write();
            } catch (IOException exception) {
                reportFailure(key, exception);
            } catch (RuntimeException exception) {
                reportFailure(key, exception);
            }
        }
    }

    private static void reportFailure(String key, Exception exception) {
        System.err.println("[HashimotoAddons] Failed to persist " + key + ": " + exception.getMessage());
        exception.printStackTrace(System.err);
    }

    interface WriteOperation {
        void write() throws IOException;
    }
}
