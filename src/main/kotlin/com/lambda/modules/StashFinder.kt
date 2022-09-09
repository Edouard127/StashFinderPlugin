package com.lambda.modules

import com.lambda.StashFinderPlugin
import com.lambda.classes.Vector2
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.event.events.PlayerTravelEvent
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.ESPRenderer
import com.lambda.client.util.items.*
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.safeListener
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.BlockPos
import java.awt.Color
import java.util.*
import kotlin.math.*


internal object StashFinder: PluginModule(
    name = "StashFinder",
    category = Category.MOVEMENT,
    description = "Searches an Area",
    pluginMain = StashFinderPlugin
) {
    private val range by setting("Range", 100.0, 1.0..1000.0, 1.0, description = "Range to search in chunks")
    private val step by setting("Step", 1, 0..20, 1, description = "Step size for searching")
    private val thickness by setting("Line Thickness", 2.0f, 0.25f..8.0f, 0.25f)

    private val renderer = ESPRenderer()
    private var playerPos = Vector2(0.0, 0.0)

    init {
        onEnable {
            playerPos = Vector2(mc.player.posX, mc.player.posZ)
            MessageSendHelper.sendChatMessage("Searching...")
        }
        safeListener<RenderWorldEvent> {
            val list = calculatePath(playerPos, range)
            drawBetween(list)
        }
        safeListener<PlayerTravelEvent> {
            elytraFly(it)
        }
    }
    private fun SafeClientEvent.drawBetween(posList: LinkedList<BlockPos>) {
        renderer.aFilled = 20
        renderer.aOutline = 255
        renderer.thickness = thickness
        posList.forEach {
            renderer.add(it, ColorHolder(Color.GREEN))
        }
        renderer.add(this.player.position, ColorHolder(Color.RED))
        renderer.render(true)
    }
    private fun SafeClientEvent.calculatePath(playerPos: Vector2, range: Double): LinkedList<BlockPos> {
        val linkedList = LinkedList<BlockPos>()
        val chunkSize = 16
        val renderDistance = mc.gameSettings.renderDistanceChunks
        val chunkPos = playerPos.div(chunkSize.toDouble()).floor()
        for (i in 0..(range * 2).toInt() step renderDistance+step) {
            val angle = i.toDouble().pow(1.2) * 0.1
            val r = sqrt(i.toDouble()) * 0.5
            val x = r * cos(angle) + chunkPos.x
            val z = r * sin(angle) + chunkPos.y
            val pos = BlockPos(x * chunkSize, 0.0, z * chunkSize)
            if (pos.getDistance(playerPos.x.toInt(), 0, playerPos.y.toInt()) <= range) {
                linkedList.add(pos)
            }
        }

        return linkedList
    }

    private val isElytraFlying: Boolean
        get() = mc.player.isElytraFlying

    private fun SafeClientEvent.elytraFly(event: PlayerTravelEvent) {
        player.motionY *= 0.98f
        connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY-0.05f, player.posZ, false))
        connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING))
        event.cancel()
    }
    private val SafeClientEvent.shouldUseFireworks: Boolean
        get() {
            // If we have a firework in our hotbar, use it
            if (player.inventory.hasItemStack(ItemStack(Items.FIREWORKS))) return true
            // If we have a firework in our offhand, use it
            if (player.inventory.offHandInventory[0].item == Items.FIREWORKS) return true
            // If we have a firework in our inventory, swap to it
            player.inventorySlots.forEachIndexed { _, itemStack ->
                if (itemStack == Items.FIREWORKS) {
                    // TODO: Swap to it
                }
            }
            return false
        }
}
