package com.entityarealogger.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;


public class EntityAreaLoggerClient implements ClientModInitializer {
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

	}

    private static int executeCommandWithArg(CommandContext<FabricClientCommandSource> context) {
	    int value = IntegerArgumentType.getInteger(context, "value");
	    context.getSource().sendFeedback(Component.literal("Called /command_with_arg with value = %s".formatted(value)));
	    return 1;
    }
}