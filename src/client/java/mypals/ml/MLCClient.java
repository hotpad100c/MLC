package mypals.ml;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static mypals.ml.CommandRegistrar.registerCommands;

public class MLCClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		registerCommands();
        ensureMLCDirectoryExists();
		// 注册聊天消息事件监听器
		ClientReceiveMessageEvents.ALLOW_CHAT.register(this::onChatMessageReceived);
	}

	private boolean onChatMessageReceived(Text originalMessage, SignedMessage signedMessage, GameProfile gameProfile, MessageType.Parameters parameters, Instant instant) {
		// 创建一个新的样式，添加点击事件和悬停事件
		Style hoverStyle = Style.EMPTY
				.withFormatting(Formatting.UNDERLINE) // 鼠标悬停时文本下划线
				.withColor(Formatting.LIGHT_PURPLE);

		// 创建提示文本
		Text hoverText = Text.literal("Click to copy").formatted(Formatting.ITALIC).setStyle(hoverStyle);

		// 创建一个新的样式，添加点击事件和悬停事件
		Text modifiedMessage = originalMessage.copy().setStyle(Style.EMPTY
				.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, originalMessage.getString()))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
		);

		// 将修改后的消息重新应用到聊天框
		MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(modifiedMessage);

		// 返回false，表示不要再处理原始消息
		return false;
	}

	private void ensureMLCDirectoryExists() {
		MinecraftClient client = MinecraftClient.getInstance();
		Path modsDirectory = Paths.get(client.runDirectory.getAbsolutePath(), "Mhelper", "mlcText");
		try {
			if (!Files.exists(modsDirectory)) {
				Files.createDirectories(modsDirectory);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}