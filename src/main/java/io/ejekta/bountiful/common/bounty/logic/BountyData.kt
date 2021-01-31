package io.ejekta.bountiful.common.bounty.logic

import io.ejekta.bountiful.common.content.BountifulContent
import io.ejekta.bountiful.common.serial.Format
import io.ejekta.bountiful.common.util.GameTime
import io.ejekta.bountiful.common.util.JsonStrict.toJson
import io.ejekta.bountiful.common.util.JsonStrict.toTag
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import net.minecraft.world.World
import java.util.*
import kotlin.math.max

@Serializable
class BountyData {

    var owner: String? = null // UUID
    var timeStarted = -1L
    var timeToComplete = -1L
    var rarity = BountyRarity.COMMON
    val objectives = mutableListOf<BountyDataEntry>()
    val rewards = mutableListOf<BountyDataEntry>()

    private fun timeLeft(world: World): Long {
        return max(timeStarted - world.time + timeToComplete, 0L)
    }

    fun save() = Format.NBT.encodeToJsonElement(serializer(), this)

    fun tryCashIn(player: PlayerEntity, stack: ItemStack) {
        val objs = objectives.map {
            it().finishObjective(this, it, player)
        }

        if (objs.all { it }) {
            rewards.forEach {
                it().giveReward(this, it, player)
            }
            stack.decrement(stack.maxCount)
        } else {
            player.sendMessage(TranslatableText("bountiful.tooltip.requirements"), false)
            println("All objectives finished but some returned false!")
        }

    }

    // ### Formatting ### //

    fun formattedTimeLeft(world: World): Text {
        return GameTime.formatTimeExpirable(timeLeft(world) / 20)
    }

    private fun formattedObjectives(): List<Text> {
        return objectives.map {
            it.formatted(this, MinecraftClient.getInstance().player!!, true)
        }
    }

    private fun formattedRewards(): List<Text> {
        return rewards.map {
            it.formatted(this, MinecraftClient.getInstance().player!!, false)
        }
    }

    fun tooltipInfo(world: World): List<Text> {
        val lines = mutableListOf<Text>()
        lines += TranslatableText("bountiful.tooltip.required").formatted(Formatting.GOLD).append(":")
        lines += formattedObjectives()
        lines += TranslatableText("bountiful.tooltip.rewards").formatted(Formatting.GOLD).append(":")
        lines += formattedRewards()
        return lines
    }

    companion object : ItemData<BountyData>(serializer()) {

        override val creator: () -> BountyData = {
            BountyData().apply {
                timeStarted = 100
                timeToComplete = 300
                rarity = BountyRarity.EPIC
                objectives.add(
                    BountyDataEntry(BountyType.ITEM, "minecraft:dirt", 2)
                )
                rewards.add(
                    BountyDataEntry(BountyType.ITEM, "minecraft:iron_ingot", 10)
                )
            }
        }

        fun default() = BountyData().apply {
            timeStarted = 100
            timeToComplete = 300
            rarity = BountyRarity.EPIC
            objectives.add(
                BountyDataEntry(BountyType.ITEM, "minecraft:dirt", 2)
            )
            rewards.add(
                BountyDataEntry(BountyType.ITEM, "minecraft:iron_ingot", 10)
            )
        }

        fun default2() = BountyData().apply {
            timeStarted = 100
            timeToComplete = 300
            rarity = BountyRarity.COMMON
            objectives.add(
                BountyDataEntry(BountyType.ITEM, "minecraft:yellow_wool", 1)
            )
            rewards.add(
                BountyDataEntry(BountyType.ITEM, "minecraft:bookshelf", 3)
            )
        }

        fun defaultRandom() = listOf(default(), default2()).random()

    }

}