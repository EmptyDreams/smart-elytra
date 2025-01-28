package top.kmar.mc.se

import net.fabricmc.api.ClientModInitializer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

object SmartElytraClient : ClientModInitializer {

	override fun onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
	}

	/**
	 * 获取指定坐标下方的第一个非空气方块的 Y 坐标
	 */
	@Suppress("NOTHING_TO_INLINE")
	@JvmStatic
	internal inline fun World.getFirstBlockPosY(x: Int, y: Int, z: Int, limit: Int): Int {
		val pos = BlockPos.Mutable(x, y, z)
		repeat(limit) {
			if (!getBlockState(pos).isAir) return pos.y
			pos.y -= 1
		}
		return Int.MIN_VALUE
	}

	/**
	 * 从指定位置向指定方向遍历指定数量个方块
	 * @param startX 起点 X 坐标
	 * @param startZ 起点 Z 坐标
	 * @param directionAngle 方向角度
	 * @param len 遍历数量
	 * @param consumer 回调函数，参数为当前遍历到的方块索引、X 坐标、Z 坐标
	 */
	@JvmStatic
	internal inline fun eachVisitedGrids(
		startX: Double, startZ: Double, directionAngle: Double,
		len: Int,
		consumer: (index: Int, x: Int, z: Int) -> Unit
	) {
		val directionInRadians = Math.toRadians(-directionAngle % 360)
		val dx = sin(directionInRadians)
		val dz = cos(directionInRadians)

		// 从起点开始
		var x = startX
		var z = startZ
		var lastX = x.toInt()
		var lastZ = z.toInt()

		repeat(len) {
			// 计算当前所在格子的坐标
			val gridX = floor(x).toInt()
			val gridZ = floor(z).toInt()
			if (gridX != lastX || gridZ != lastZ) {
				consumer(it, gridX, gridZ)
				lastX = gridX
				lastZ = gridZ
			}
			// 更新位置
			x += dx
			z += dz
		}
	}

}