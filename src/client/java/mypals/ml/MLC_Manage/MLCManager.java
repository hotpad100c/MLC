package mypals.ml.MLC_Manage;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
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
                    List<String> lines = new ArrayList<>();
                    String line;

                    // 读取文件所有内容到列表
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }

                    // 处理读取到的内容
                    processLines(lines, client);

                } catch (IOException e) {
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

    private static void processLines(List<String> lines, MinecraftClient client) {
        try {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                synchronized (pauseLock) {
                    if (paused) {
                        try {
                            pauseLock.wait(); // 将线程阻塞，直到另一个方法调用 pauseLock.notifyAll()
                        } catch (InterruptedException ex) {
                            break;
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
                } else if (line.startsWith("#FOR")) {
                    try {
                        // 提取重复次数
                        int repeatCount = Integer.parseInt(line.replaceAll("\\D+", ""));
                        List<String> loopContent = new ArrayList<>();

                        // 寻找ENDFOR
                        int nestedLoops = 0;
                        while (++i < lines.size()) {
                            String loopLine = lines.get(i);
                            if (loopLine.startsWith("#FOR")) {
                                nestedLoops++;
                            } else if (loopLine.startsWith("#ENDFOR")) {
                                if (nestedLoops == 0) {
                                    break;
                                } else {
                                    nestedLoops--;
                                }
                            }
                            loopContent.add(loopLine);
                        }

                        // 递归处理循环内容
                        for (int j = 0; j < repeatCount; j++) {
                            processLines(loopContent, client);
                        }

                    } catch (NumberFormatException e) {
                        System.err.println("Invalid repeat count in line: " + line);
                    }
                } else if (!line.startsWith("#ENDFOR")) {
                    if (line.startsWith("/")) {
                        // 发送命令
                        client.player.networkHandler.sendCommand(line.substring(1));
                    } else {
                        // 发送聊天消息
                        client.player.networkHandler.sendChatMessage(line);
                    }
                    Thread.sleep(SpeakDelay);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
    public static void openMhelperFolder() {
        MinecraftClient client = MinecraftClient.getInstance();
        Path filePath = Paths.get(client.runDirectory.getAbsolutePath(), "Mhelper", "mlcText");
            try {
                // 打开文件夹
                Desktop.getDesktop().open(filePath.toFile());
                String m = "MLC folder in path "+ filePath +"is now opened.";
                sendNotification(m, Formatting.GREEN);
            } catch (IOException e) {
                String m = "MLC folder was lost!WHAT HAVE YOU DONE???";
                sendNotification(m, Formatting.RED);
                e.printStackTrace();
                System.err.println("Failed to open MLC folder.");
            }
    }
    public static void openMhelperFolderWithName(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        Path filePath = Paths.get(client.runDirectory.getAbsolutePath(), "Mhelper", "mlcText", name + ".txt");
        try {
            // 打开文件夹
            Desktop.getDesktop().open(filePath.toFile());
            String m = "File<"+name+"> is now opened!";
            sendNotification(m, Formatting.GREEN);
        } catch (IOException e) {
            String m = "File<"+name+"> does not exists!";
            sendNotification(m, Formatting.RED);
            e.printStackTrace();
            System.err.println("Failed to open MLC file.");
        }
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
