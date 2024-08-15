package mypals.ml.MLC_Manage;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

import static mypals.ml.MLC_Manage.VariableManager.replaceVariables;

public class MLCProcessor {

    private static final Object pauseLock = new Object();
    private static boolean paused = false;

    public void processLines(List<String> lines, MinecraftClient client) {
        try {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                synchronized (pauseLock) {
                    if (paused) {
                        try {
                            pauseLock.wait(); // 阻塞线程，直到恢复
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                }

                if (line.startsWith("#DELAY")) {
                    line = replaceVariables(line);
                    try {
                        MLCManager.SpeakDelay = Integer.parseInt(line.replaceAll("\\D+", ""));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid delay value in line: " + line);
                    }
                } else if (line.startsWith("#INT") || line.startsWith("#STRING") ||
                        line.startsWith("#BOOL") || line.startsWith("#FLOAT")) {
                    VariableManager.parseVariable(line); // 解析并设置变量
                } else if (line.startsWith("#FOR")) {
                    line = replaceVariables(line);
                    try {
                        int repeatCount = Integer.parseInt(line.replaceAll("\\D+", ""));
                        List<String> loopContent = new ArrayList<>();
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
                        for (int j = 0; j < repeatCount; j++) {
                            processLines(loopContent, client);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid repeat count in line: " + line);
                    }
                } else if (!line.startsWith("#ENDFOR")) {
                    line = replaceVariables(line); // 替换变量

                    if (line.startsWith("/")) {
                        client.player.networkHandler.sendCommand(line.substring(1));
                    } else {
                        client.player.networkHandler.sendChatMessage(line);
                    }

                    Thread.sleep(MLCManager.SpeakDelay);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void pauseAll() {
        synchronized (pauseLock) {
            paused = true;
            sendNotification("Paused....", Formatting.RED);
        }
    }

    public static void resumeAll() {
        synchronized (pauseLock) {
            paused = false;
            sendNotification("Resumed....", Formatting.LIGHT_PURPLE);
            pauseLock.notifyAll();
        }
    }
    public static void sendNotification(String message, Formatting color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text chatMessage = Text.literal(message).styled(style -> style.withColor(color));
            client.player.sendMessage(chatMessage, false); // false 表示消息不会显示在命令输出中
        }
    }
}
