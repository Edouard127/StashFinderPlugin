package com.lambda.modules

import com.lambda.StashFinderPlugin
import com.lambda.classes.Vector2
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.event.events.PlayerTravelEvent
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.module.Category
import com.lambda.client.module.modules.movement.ElytraFlight
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.ESPRenderer
import com.lambda.client.util.items.*
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.world.getGroundPos
import net.minecraft.entity.item.EntityFireworkRocket
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemFirework
import net.minecraft.util.EnumHand
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
    private val step by setting("Step", 1, 0..20, 1, description = "Step size for searching")
    private val searchRadius by setting("Search Radius", 2.0, 1.0..50.0, 1.0, description = "Radius to search in chunks")
    private val hand by setting("Hand", Hand.MAIN_HAND, description = "Hand to use")
    private val thickness by setting("Line Thickness", 2.0f, 0.25f..8.0f, 0.25f)

    private val renderer = ESPRenderer()
    private var playerPos = Vector2(0.0, 0.0)
    private var flying = false

    init {
        onEnable {
            playerPos = Vector2(mc.player.posX, mc.player.posZ)
            MessageSendHelper.sendChatMessage("Calculating path...")
        }
        safeListener<RenderWorldEvent> {
            val list = calculatePath(playerPos, searchRadius)
            drawBetween(list)
        }
        safeListener<PlayerTravelEvent> {
            flying = player.isElytraFlying
            if (!flying) {
                ElytraFlight.mode.value = ElytraFlight.ElytraFlightMode.BOOST
                ElytraFlight.enable()
            } else update()
        }
    }
    private fun drawBetween(posList: LinkedList<BlockPos>) {
        renderer.aFilled = 20
        renderer.aOutline = 255
        renderer.thickness = thickness
        posList.forEach {
            renderer.add(it, ColorHolder(Color.GREEN))
        }
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
    private fun SafeClientEvent.update() {
        val armorSlot = mc.player.inventory.armorInventory[2]
        if(armorSlot.item != Items.ELYTRA) {
            MessageSendHelper.sendChatMessage("$chatName You need to be wearing an Elytra to use this module!")
            disable()
        }
        val angle = Math.toRadians(player.rotationPitch.toDouble())
        val closestBlock = getClosestBlock()
        val altitude = player.posY - closestBlock.y
        val glideRange = altitude / tan(angle)
        if (glideRange < 50) {
            useFirework()
            if (player.rotationPitch !in -25.0..-26.0) {
                if (player.rotationPitch < -25.0) {
                    player.rotationPitch += 1.0f
                } else {
                    player.rotationPitch -= 1.0f
                }
            }
        }
        val entity = world.loadedEntityList.find { it is EntityFireworkRocket }
        if (entity == null) {
            if (player.rotationPitch !in 25.0..26.0) {
                if (player.rotationPitch < 25.0) {
                    player.rotationPitch += 1.0f
                } else {
                    player.rotationPitch -= 1.0f
                }
            }
        }
    }

    private fun SafeClientEvent.useFirework() {
        moveFirework(hand.hand)
        playerController.processRightClick(player, world, hand.hand)
    }
    private fun List<Slot>.getFireworkSlot(): Slot? {
        return this.firstItem<ItemFirework, Slot>()
    }
    private fun SafeClientEvent.moveFirework(hand: EnumHand) {
        val slotFrom = player.inventorySlots.getFireworkSlot() ?: return
        if (player.hotbarSlots.contains(slotFrom)) return swapToSlot(slotFrom.slotNumber)
        return when(hand) {
            EnumHand.MAIN_HAND -> moveToHotbar(this@StashFinder, slotFrom) { it.item !is ItemFirework }
            EnumHand.OFF_HAND -> moveToSlot(this@StashFinder, slotFrom, player.offhandSlot)
        }
    }
    private fun SafeClientEvent.getClosestBlock() = mc.world.getGroundPos(player)

    @Suppress("UNUSED")
    private enum class Hand(val hand: EnumHand) {
        MAIN_HAND(hand = EnumHand.MAIN_HAND), OFF_HAND(hand = EnumHand.OFF_HAND)
    }

}
