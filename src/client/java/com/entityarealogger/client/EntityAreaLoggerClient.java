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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;


public class EntityAreaLoggerClient implements ClientModInitializer {


	private AABB area = null;
	private BufferedWriter writer = null;
	private long tickCounter = 0;

	private boolean triggering = false;
	private boolean triggerIsActive = false;
	private CommandContext<FabricClientCommandSource> triggerContext = null;
	private BlockPos triggerBlock = null;
	private BlockState lastBlockState = null;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

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
				.then(
					ClientCommands.literal("trigger")
					.then(ClientCommands.argument("x1", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("y1", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("z1", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("x2", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("y2", DoubleArgumentType.doubleArg())
					.then(ClientCommands.argument("z2", DoubleArgumentType.doubleArg())
					.then(ClientCommands.literal("on")
					.then(ClientCommands.argument("xt", IntegerArgumentType.integer())
					.then(ClientCommands.argument("yt", IntegerArgumentType.integer())
					.then(ClientCommands.argument("zt", IntegerArgumentType.integer())
					.executes(this::setTrigger)
				)))))))))))
				.then(
					ClientCommands.literal("trigger")
					.then(ClientCommands.literal("clear")
					.executes(this::clearTrigger)
				))
			);
		});

		//Log On Tick
		ClientTickEvents.END_CLIENT_TICK.register(this::logOnTick);
		ClientTickEvents.END_CLIENT_TICK.register(this::triggerOnTick);

		//Close File on Disconnect
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> closeLogger());
		

	}

	private int startLogger(CommandContext<FabricClientCommandSource> context) {

		if (writer != null) stopLogger(context);
		tickCounter = 0;
		
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

		triggerIsActive = false;

		context.getSource().sendFeedback(Component.literal("Logging Stopped"));
		return 1;
	}

	private void closeLogger() {
		if (writer == null) return;
		try {
			writer.close();
		} catch (IOException ignored) {
		}
		writer = null;
		area = null;
		triggering = false;
		triggerIsActive = false;
	}

	private void logOnTick(Minecraft client) {
		if (writer == null || area == null || client.level == null) return;

		tickCounter++;

		//Only write 2 times per second
		if (tickCounter%10 != 0) return;

		List<Entity> entities = client.level.getEntities((Entity) null, area);
 
		Map<String, Integer> counts = new TreeMap<>();
		for (Entity e : entities) {
			String type = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
			counts.merge(type, 1, Integer::sum);
		}
	
		long worldTime = client.level.getGameTime();
		try {
			if (counts.isEmpty()) {
				// keep every tick represented in the file
				writer.write(tickCounter + "," + worldTime + ",none,0\n");
			} else {
				for (Map.Entry<String, Integer> entry : counts.entrySet()) {
					writer.write(tickCounter + "," + worldTime + "," + entry.getKey() + "," + entry.getValue() + "\n");
				}
			}
			writer.flush();
		} catch (IOException e) {
			//context.getSource().sendFeedback("Write error, stopping: " + e.getMessage());
			closeLogger();
		}

	}

	private int setTrigger(CommandContext<FabricClientCommandSource> context) {

		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return 0;

		triggering = true;
		triggerContext = context;

		double x = IntegerArgumentType.getInteger(context, "xt");
		double y = IntegerArgumentType.getInteger(context, "yt");
		double z = IntegerArgumentType.getInteger(context, "zt");
		triggerBlock = new BlockPos((int) x, (int) y, (int) z);
		lastBlockState = level.getBlockState(triggerBlock);

		context.getSource().sendFeedback(Component.literal(String.format("Set logging trigger on Block %s %s %s.", x, y, z)));

		return 1;
	}

	private int clearTrigger(CommandContext<FabricClientCommandSource> context) {

		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return 0;

		stopLogger(context);
		triggerContext = null;
		triggerBlock = null;

		context.getSource().sendFeedback(Component.literal("Cleaed logging triggers."));

		return 1;
	}

	private void triggerOnTick(Minecraft client) {

			if (!triggering || triggerBlock == null) return;

			ClientLevel level = Minecraft.getInstance().level;
			if (level == null) return;

			BlockState currenBlockState = level.getBlockState(triggerBlock);
			if (currenBlockState != lastBlockState) {
				lastBlockState = currenBlockState;

				if (!triggerIsActive) {
					triggerIsActive = true;
					startLogger(triggerContext);
				} else {
					stopLogger(triggerContext);
				}
			}
	}

}