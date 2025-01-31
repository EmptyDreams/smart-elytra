package top.kmar.mc.se

import com.mojang.logging.LogUtils
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.slf4j.Logger
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object SmartElytraClient : ClientModInitializer {

	@JvmStatic
	val logger: Logger = LogUtils.getLogger()

	@JvmStatic
	internal var playerJumpTimestamp = 0L

	override fun onInitializeClient() {
		EntityElytraEvents.ALLOW.register { player ->
			player !is PlayerEntity || player.checkFallFlyingCustom()
		}
		logger.atInfo().log("SmartElytra Client Initialized!")
	}

	@JvmStatic
	private fun PlayerEntity.checkFallFlyingCustom(): Boolean {
		if (getStackInHand(Hand.MAIN_HAND).isOf(Items.FIREWORK_ROCKET) ||
			getStackInHand(Hand.OFF_HAND).isOf(Items.FIREWORK_ROCKET)
		) {
			// 如果玩家手持烟花则允许飞行
			return true
		}

		val playerBlockPos = blockPos
		val world = world

		if (!isSprinting && !world.getBlockState(playerBlockPos).isAir) {
			// 如果玩家脚下不是空气，说明玩家卡在了方块里面或者在流体中，则不允许飞行
			println(1)
			return false
		}

		if (abs(velocity.y) < 0.25 && world.time - playerJumpTimestamp < 20) {
			return true
		}

		if (!isSprinting) {
			val downState = world.getBlockState(playerBlockPos.down())
			// 如果玩家不在奔跑且按下空格的时机比较早说明可能在尝试钻洞或翻墙
			// 如果玩家不在奔跑且脚下是流体，说明可能在尝试翻墙
			if (downState.isAir || !downState.fluidState.isEmpty) {
				return true
			}
		}

		// 检查玩家正前方的地形是否允许飞行
		// 检查方法比较简单，判断前方五个方块的高度是否满足 [1, 1, 2, 3, 4]
		val y = playerBlockPos.y - 1
		val directionInRadians = Math.toRadians((-yaw % 360).toDouble())
		val dx = sin(directionInRadians)
		val dz = cos(directionInRadians)

		// 从起点开始
		var x = pos.x
		var z = pos.z
		var lastX = x.toInt()
		var lastZ = z.toInt()

		for (i in 0..4) {
			// 计算当前所在格子的坐标
			val gridX = x.toInt()
			val gridZ = z.toInt()
			if (gridX != lastX || gridZ != lastZ) {
				val firstBlockPosY = getFirstBlockPosY(
					world, gridX, y, gridZ, max(i, 1)
				)
				if (firstBlockPosY != Int.Companion.MIN_VALUE) {
					if (i == 0 || i == 1) {
						if (y - firstBlockPosY == 0) {
							return false
						}
					} else if (y - firstBlockPosY < i) {
						return false
					}
				}
				lastX = gridX
				lastZ = gridZ
			}
			// 更新位置
			x += dx
			z += dz
		}
		return true
	}

	/**
	 * 获取指定坐标下方的第一个非空气方块的 Y 坐标
	 */
	@JvmStatic
	private fun getFirstBlockPosY(world: World, x: Int, y: Int, z: Int, limit: Int): Int {
		val pos = BlockPos.Mutable(x, y, z)
		repeat(limit) {
			val posY = pos.y
			if (!world.getBlockState(pos).isAir) return posY
			pos.y = posY - 1
		}
		return Int.Companion.MIN_VALUE
	}

}