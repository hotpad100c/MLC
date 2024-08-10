package mypals.ml;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mypals.ml.MLC_Manage.MLCManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;

import java.util.List;
import java.util.stream.Collectors;

public class CommandRegistrar {

    // 线程名称建议
    static SuggestionProvider<FabricClientCommandSource> MLCSuggestions = (context, builder) -> {
        List<String> MLCNames = MLCManager.getMLCFileNames();
        return CommandSource.suggestMatching(MLCNames, builder);
    };

    // 线程标记建议
    static SuggestionProvider<FabricClientCommandSource> MlCRunningThreads = (context, builder) -> {
        String name = StringArgumentType.getString(context, "name");
        List<String> markers = MLCManager.getThreadMarkers(name);
        return CommandSource.suggestMatching(markers, builder);
    };

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(ClientCommandManager.literal("MLC")
                    .then(ClientCommandManager.literal("load")
                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .suggests(MLCSuggestions)
                                    .executes(context -> {
                                        String name = StringArgumentType.getString(context, "name");
                                        MLCManager.sendMlc(name);
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("stop")
                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .suggests(MLCSuggestions)
                                    .then(ClientCommandManager.argument("marker", StringArgumentType.word())
                                            .suggests(MlCRunningThreads)
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                String marker = StringArgumentType.getString(context, "marker");
                                                MLCManager.stopThread(name, marker);
                                                return 1;
                                            })
                                    )
                            )
                    )
            );
        });
    }
}
