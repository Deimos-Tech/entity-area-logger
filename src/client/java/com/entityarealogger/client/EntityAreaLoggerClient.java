package com.entityarealogger.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;


public class EntityAreaLoggerClient implements ClientModInitializer {


	private AABB area = null;
	private BufferedWriter writer = null;
	private long tickCounter = 0;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
	        dispatcher.register(ClientCommands.literal("clienttater").executes(context -> {
		        context.getSource().sendFeedback(Component.literal("Called /clienttater with no arguments."));
		        return 1;
	        }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
	        dispatcher.register(ClientCommands.literal("command_with_arg")
			.then(ClientCommands.argument("value", IntegerArgumentType.integer())
			.executes(EntityAreaLoggerClient::executeCommandWithArg)));
            });

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
	        dispatcher.register(ClientCommands.literal("logentities")
				.then(
					ClientCommands.literal("start")
					.then(ClientCommands.argument("x1", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("y1", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("z1", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("x2", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("y2", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("z2", DoubleArgumentType.doubleArg())
					.executes(this::startLogger)
				)))))))
				//This should be rewritten to accept block position to enable autocomplete
				//However communication issues with unmodded servers might be possible
				.then(
					ClientCommands.literal("stop")
					.executes(this::stopLogger)
				)
			);
        });

		

	}

    private static int executeCommandWithArg(CommandContext<FabricClientCommandSource> context) {
	    int value = IntegerArgumentType.getInteger(context, "value");
	    context.getSource().sendFeedback(Component.literal("Called /command_with_arg with value = %s".formatted(value)));
	    return 1;
    }


    private int startLogger(CommandContext<FabricClientCommandSource> context) {

		if (writer != null) stopLogger(context);
		
		double x1 = DoubleArgumentType.getDouble(context, "x1");
        double y1 = DoubleArgumentType.getDouble(context, "y1");
        double z1 = DoubleArgumentType.getDouble(context, "z1");
        double x2 = DoubleArgumentType.getDouble(context, "x2");
        double y2 = DoubleArgumentType.getDouble(context, "y2");
        double z2 = DoubleArgumentType.getDouble(context, "z2");

		area = new AABB(x1, y1, z1, x2, y2, z2);

	    context.getSource().sendFeedback(Component.literal("Started Logger within = %s %s %s / %s %s %s".formatted(x1, y1, z1, x2, y2, z2)));

        try {
            Path dir = FabricLoader.getInstance().getGameDir().resolve("entity_logs");
            Files.createDirectories(dir);
            String name = "entities_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".csv";
            Path file = dir.resolve(name);
            writer = Files.newBufferedWriter(file);
            writer.write("tick,world_time,entity_type,count\n");
            context.getSource().sendFeedback(Component.literal("Started logging entities to " + file));
        } catch (IOException e) {
            writer = null;
            area = null;
            context.getSource().sendFeedback(Component.literal("Failed to open log file: " + e.getMessage()));
        }

	    return 1;
    }

    private int stopLogger(CommandContext<FabricClientCommandSource> context) {

		if (writer == null) {
			context.getSource().sendFeedback(Component.literal("Could not stop logger: Logger not running."));
			return 0;
		}
		
		try {
			writer.close();
		} catch (IOException ignored) {
        }

		writer = null;
		area = null;

	    context.getSource().sendFeedback(Component.literal("Logging Stopped"));
	    return 1;
    }
}