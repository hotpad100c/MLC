package mypals.ml.MLC_Manage;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.io.InputStreamReader;

public class MLCManager {
    public static int SpeakDelay = 100;
    private static final ConcurrentMap<String, Thread> runningThreads = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> threadMarkers = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Thread> threadMap = new ConcurrentHashMap<>();
    private static int threadCounter = 0; // 用于生成唯一标识符

    private static final Object pauseLock = new Object();

    private static boolean paused = false;

    public static List<String> getThreadMarkers(String name) {
        List<String> markers = new ArrayList<>();
        for (String key : runningThreads.keySet()) {
            if (key.startsWith(name)) {
                markers.add(key.substring(name.length()));
            }
        }
        return markers;
    }

    public static void sendMlc(String name) {
        String marker = generateUniqueMarker();

        Thread thread = new Thread(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            Path filePath = Paths.get(client.runDirectory.getAbsolutePath(), "Mhelper", "mlcText", name + ".txt");
            if (filePath.toFile().exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                Files.newInputStream(filePath), StandardCharsets.UTF_8))) { // 使用UTF-8编码
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (pauseLock) {
                            if (paused) {
                                try {
                                    pauseLock.wait();
                                    // 将此线程阻塞，直到另一个方法调用 pauseLock.notifyAll()
                                    // 注意，调用 wait() 会释放此线程在 pauseLock 上持有的同步锁
                                    // 这样，另一个线程就可以获取锁并调用 notifyAll()
                                } catch (InterruptedException ex) {
                                    break;//芜湖
                                }
                            }
                        }
                            if (line.startsWith("#DELAY")) {
                                try {
                                    // 提取DELAY后面的数字并设置为SpeakDelay
                                    SpeakDelay = Integer.parseInt(line.replaceAll("\\D+", ""));
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid delay value in line: " + line);
                                }
                            } else if (!line.trim().isEmpty()) {
                                if (line.startsWith("/")) {
                                    // 发送命令
                                    client.player.networkHandler.sendCommand(line.substring(1));
                                } else {
                                    // 发送聊天消息
                                    client.player.networkHandler.sendChatMessage(line);
                                }
                                // 应用延迟
                                Thread.sleep(SpeakDelay);
                            }

                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    runningThreads.remove(name + marker); // 移除线程
                    threadMarkers.remove(name + marker); // 移除线程标记
                }
            } else {
                System.err.println("File not found: " + filePath.toString());
            }
        }, "MLC-Thread-" + name + "-" + marker); // 设置线程名称

        runningThreads.put(name + marker, thread); // 记录线程
        threadMarkers.put(name + marker, marker); // 记录线程标记
        thread.start();
    }

    public static void stopThread(String name, String marker) {
        String key = name + marker;
        Thread thread = runningThreads.get(key);
        if (thread != null) {
            thread.interrupt(); // 停止线程
            runningThreads.remove(key); // 从记录中移除
            threadMarkers.remove(key); // 从记录中移除标记
        } else {
            System.err.println("No thread found with name: " + name + " and marker: " + marker);
        }
    }

    public static List<String> getMLCFileNames() {
        MinecraftClient client = MinecraftClient.getInstance();
        Path modsDirectory = Paths.get(client.runDirectory.getAbsolutePath(), "Mhelper", "mlcText");
        if (Files.exists(modsDirectory)) {
            File[] files = modsDirectory.toFile().listFiles((dir, name) -> name.endsWith(".txt"));
            if (files != null) {
                List<String> artNames = new ArrayList<>();
                for (File file : files) {
                    String artName = file.getName().replace(".txt", "");
                    artNames.add(artName);
                }
                return artNames;
            }
        }
        return Collections.emptyList();
    }

    public static void stopAllThreads() {
        runningThreads.forEach((name, thread) -> {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        });
        runningThreads.clear();
    }

    public static List<String> getRunningThreadNames() {
        return new ArrayList<>(runningThreads.keySet());
    }

    private static String generateUniqueMarker() {
        return String.valueOf(threadCounter++); // 生成唯一标识符
    }

    public static void pauseAll() {
        paused = true;
        String m = "Paused all running threads.";
        sendNotification(m, Formatting.RED);
    }

    public static void resumeAll() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // 解锁
        }
        String m = "Resumed all running threads.";
        sendNotification(m, Formatting.LIGHT_PURPLE);
    }

    public static void sendNotification(String message, Formatting color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text chatMessage = Text.literal(message).styled(style -> style.withColor(color));
            client.player.sendMessage(chatMessage, false); // false 表示消息不会显示在命令输出中
        }
    }
}
