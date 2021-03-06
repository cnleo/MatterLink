package matterlink

import kotlinx.coroutines.runBlocking
import matterlink.config.cfg
import matterlink.handlers.ChatEvent
import matterlink.handlers.ChatProcessor
import matterlink.handlers.DeathHandler
import matterlink.handlers.JoinLeaveHandler
import matterlink.handlers.ProgressHandler
import matterlink.handlers.TickHandler
import net.minecraft.command.server.CommandBroadcast
import net.minecraft.command.server.CommandEmote
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraftforge.event.CommandEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.AdvancementEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

//FORGE-DEPENDENT
@Mod.EventBusSubscriber
object EventHandler {

    //MC-VERSION & FORGE DEPENDENT
    @SubscribeEvent
    @JvmStatic
    fun progressEvent(e: AdvancementEvent) = runBlocking {
        if (e.advancement.display == null) return@runBlocking
        ProgressHandler.handleProgress(
            name = e.entityPlayer.displayName.unformattedText,
            message = "has made the advancement",
            display = e.advancement.displayText.unformattedText,
            x = e.entityPlayer.posX.toInt(),
            y = e.entityPlayer.posY.toInt(),
            z = e.entityPlayer.posZ.toInt(),
            dimension = e.entityPlayer.dimension
        )
    }

    //FORGE-DEPENDENT
    @SubscribeEvent
    @JvmStatic
    fun chatEvent(e: ServerChatEvent) = runBlocking {
        if (e.isCanceled) return@runBlocking
        e.isCanceled = ChatProcessor.sendToBridge(
            user = e.player.displayName.unformattedText,
            msg = e.message,
            x = e.player.posX.toInt(),
            y = e.player.posY.toInt(),
            z = e.player.posZ.toInt(),
            dimension = e.player.dimension,
            event = ChatEvent.PLAIN,
            uuid = e.player.gameProfile.id
        )
    }

    //FORGE-DEPENDENT
    @SubscribeEvent
    @JvmStatic
    fun commandEvent(e: CommandEvent) = runBlocking {

        val sender = when {
            e.sender is DedicatedServer -> cfg.outgoing.systemUser
            else -> e.sender.displayName.unformattedText
        }
        val args = e.parameters.joinToString(" ")
        val type = with(e.command) {
            when {
                this is CommandEmote || name.equals("me", true) -> ChatEvent.ACTION
                this is CommandBroadcast || name.equals("say", true) -> ChatEvent.BROADCAST
                else -> return@runBlocking
            }
        }
        ChatProcessor.sendToBridge(
            user = sender,
            msg = args,
            event = type,
            x = e.sender.position.x,
            y = e.sender.position.y,
            z = e.sender.position.z,
            dimension = when {
                e.sender is DedicatedServer -> null
                else -> e.sender.commandSenderEntity?.dimension ?: e.sender.entityWorld.provider.dimension
            }
        )
    }

    //FORGE-DEPENDENT
    @SubscribeEvent
    @JvmStatic
    fun deathEvent(e: LivingDeathEvent) = runBlocking {
        if (e.entityLiving is EntityPlayer) {
            DeathHandler.handleDeath(
                player = e.entityLiving.displayName.unformattedText,
                deathMessage = e.entityLiving.combatTracker.deathMessage.unformattedText,
                damageType = e.source.damageType,
                x = e.entityLiving.posX.toInt(),
                y = e.entityLiving.posY.toInt(),
                z = e.entityLiving.posZ.toInt(),
                dimension = e.entityLiving.dimension
            )
        }
    }

    //FORGE-DEPENDENT
    @SubscribeEvent
    @JvmStatic
    fun joinEvent(e: PlayerEvent.PlayerLoggedInEvent) = runBlocking {
        JoinLeaveHandler.handleJoin(
            player = e.player.displayName.unformattedText,
            x = e.player.posX.toInt(),
            y = e.player.posY.toInt(),
            z = e.player.posZ.toInt(),
            dimension = e.player.dimension
        )
    }

    //FORGE-DEPENDENT
    @SubscribeEvent
    @JvmStatic
    fun leaveEvent(e: PlayerEvent.PlayerLoggedOutEvent) = runBlocking {
        JoinLeaveHandler.handleLeave(
            player = e.player.displayName.unformattedText,
            x = e.player.posX.toInt(),
            y = e.player.posY.toInt(),
            z = e.player.posZ.toInt(),
            dimension = e.player.dimension
        )
    }

    //FORGE-DEPENDENT
    @SubscribeEvent
    @JvmStatic
    fun serverTickEvent(e: TickEvent.ServerTickEvent) = runBlocking {
        if (e.phase == TickEvent.Phase.END)
            TickHandler.handleTick()

    }
}