package mypals.ml;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static mypals.ml.CommandRegistrar.registerCommands;

public class MLCClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		registerCommands();
        ensureMLCDirectoryExists();
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