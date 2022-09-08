package com.lambda.modules

import com.lambda.StashFinderPlugin
import com.lambda.classes.Vector2
import com.lambda.client.event.SafeClientEvent
import com.lambda.client.event.events.RenderWorldEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.ESPRenderer
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.runSafe
import com.lambda.client.util.threads.safeListener
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


internal object StashFinder: PluginModule(
    name = "StashFinder",
    category = Category.MOVEMENT,
    description = "Searches an Area",
    pluginMain = StashFinderPlugin
) {
    private val range by setting("Range", 100.0, 1.0..1000.0, 1.0, description = "Range to search in chunks")
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
        val renderDistance = mc.gameSettings.renderDistanceChunks
        val chunkSize = 16
        val radiusBlocks = range * chunkSize
        val chunkPos = playerPos.div(chunkSize.toDouble()).floor()
        val chunkPosMin = chunkPos.add(Vector2(-radiusBlocks, -radiusBlocks))
        val chunkPosMax = chunkPos.add(Vector2(radiusBlocks, radiusBlocks))
        for (x in chunkPosMin.x.toInt()..chunkPosMax.x.toInt()) {
            for (z in chunkPosMin.y.toInt()..chunkPosMax.y.toInt()) {
                val pX = x.toDouble()
                val pZ = z.toDouble()
                val chunk = Vector2(pX, pZ)
                val chunkCenter = chunk.add(Vector2(0.5, 0.5)).mul(chunkSize.toDouble())
                val distance = playerPos.distance(chunkCenter)
                if (distance <= radiusBlocks) {
                    for (i in range.toInt() - renderDistance..range.toInt() + renderDistance) {
                        val interval = (2 * Math.PI) / (range - renderDistance)
                        val angle = atan2(chunkCenter.y - playerPos.y, chunkCenter.x - playerPos.x)
                        val dstX = cos(angle + interval * i) * distance + playerPos.x
                        val dstY = sin(angle + interval * i) * distance + playerPos.y
                        val chunkPath = Vector2(dstX, dstY).div(chunkSize.toDouble()).floor()
                        val blockPos = BlockPos(chunkPath.x * chunkSize, this.player.position.y.toDouble(), chunkPath.y * chunkSize)
                        if (!linkedList.contains(blockPos)) {
                            linkedList.add(blockPos)
                        }
                    }
                }
            }
        }
        MessageSendHelper.sendChatMessage("${linkedList.first} ${linkedList.last}")
        return linkedList
    }
}
