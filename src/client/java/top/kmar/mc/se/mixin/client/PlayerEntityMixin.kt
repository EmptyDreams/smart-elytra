package top.kmar.mc.se.mixin.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.util.Hand
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import top.kmar.mc.se.SmartElytraClient
import top.kmar.mc.se.SmartElytraClient.getFirstBlockPosY
import kotlin.math.absoluteValue

@Suppress("unused")
@Environment(value = EnvType.CLIENT)
@Mixin(PlayerEntity::class)
class PlayerEntityMixin {

    @Unique
    private var jumpTimestamp = 0L

    @Inject(
        method = ["jump()V"],
        at = [At("HEAD")]
    )
    private fun jump(callback: CallbackInfo) {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val player = this as PlayerEntity
        jumpTimestamp = player.world.time
    }

    @Inject(
        method = ["checkFallFlying()Z"],
        at = [At("INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;startFallFlying()V")],
        cancellable = true
    )
    private fun checkFallFlying(callback: CallbackInfoReturnable<Boolean>) {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val player = this as PlayerEntity
        if (
            player.getStackInHand(Hand.MAIN_HAND).isOf(Items.FIREWORK_ROCKET) ||
            player.getStackInHand(Hand.OFF_HAND).isOf(Items.FIREWORK_ROCKET)
        ) { // 如果玩家手持烟花则允许飞行
            return
        }

        val playerBlockPos = player.blockPos
        val world = player.world

        if (!player.isSprinting && !world.getBlockState(playerBlockPos).isAir) {
            // 如果玩家脚下不是空气，说明玩家卡在了方块里面或者在流体中，则不允许飞行
            callback.returnValue = false
            return
        }
        println(world.time - jumpTimestamp)
        if (player.velocity.y.absoluteValue < 0.25 && world.time - jumpTimestamp < 20) {
            return
        }

        if (!player.isSprinting) {
            val downState = world.getBlockState(playerBlockPos.down())
            // 如果玩家不在奔跑且按下空格的时机比较早说明可能在尝试钻洞或翻墙
            // 如果玩家不在奔跑且脚下是流体，说明可能在尝试翻墙
            if (downState.isAir || !downState.fluidState.isEmpty) {
                return
            }
        }

        // 检查玩家正前方的地形是否允许飞行
        // 检查方法比较简单，判断前方五个方块的高度是否满足 [1, 1, 2, 3, 4]
        val y = playerBlockPos.y - 1
        SmartElytraClient.eachVisitedGrids(
            player.pos.x, player.pos.z, player.yaw.toDouble(), 5
        ) { i, x, z ->
            val firstBlockPosY = world.getFirstBlockPosY(x, y, z, i.coerceAtLeast(1))
            if (firstBlockPosY != Int.MIN_VALUE) {
                if (i == 0 || i == 1) {
                    if (y - firstBlockPosY == 0) {
                        callback.returnValue = false
                        return
                    }
                } else if (y - firstBlockPosY < i) {
                    callback.returnValue = false
                    return
                }
            }
        }
    }

}