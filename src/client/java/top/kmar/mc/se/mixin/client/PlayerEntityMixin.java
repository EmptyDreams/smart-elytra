package top.kmar.mc.se.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Unique
    private long jumpTimestamp = 0L;

    @Inject(
            method = "jump()V",
            at = @At("HEAD")
    )
    private void jump(CallbackInfo callbackInfo) {
        //noinspection DataFlowIssue
        PlayerEntity player = (PlayerEntity) (Object) this;
        jumpTimestamp = player.getWorld().getTime();
    }

    @Inject(
            method = "checkFallFlying()Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;startFallFlying()V"),
            cancellable = true
    )
    private void checkFallFlying(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {

        //noinspection DataFlowIssue
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (
                player.getStackInHand(Hand.MAIN_HAND).isOf(Items.FIREWORK_ROCKET) ||
                        player.getStackInHand(Hand.OFF_HAND).isOf(Items.FIREWORK_ROCKET)
        ) {
            // 如果玩家手持烟花则允许飞行
            return;
        }

        var playerBlockPos = player.getBlockPos();
        var world = player.getWorld();

        if (!player.isSprinting() && !world.getBlockState(playerBlockPos).isAir()) {
            // 如果玩家脚下不是空气，说明玩家卡在了方块里面或者在流体中，则不允许飞行
            callbackInfoReturnable.setReturnValue(false);
            return;
        }

        if (Math.abs(player.getVelocity().y) < 0.25 && world.getTime() - jumpTimestamp < 20) {
            return;
        }

        if (!player.isSprinting()) {
            var downState = world.getBlockState(playerBlockPos.down());
            // 如果玩家不在奔跑且按下空格的时机比较早说明可能在尝试钻洞或翻墙
            // 如果玩家不在奔跑且脚下是流体，说明可能在尝试翻墙
            if (downState.isAir() || !downState.getFluidState().isEmpty()) {
                return;
            }
        }

        // 检查玩家正前方的地形是否允许飞行
        // 检查方法比较简单，判断前方五个方块的高度是否满足 [1, 1, 2, 3, 4]
        int y = playerBlockPos.getY() - 1;
        var directionInRadians = Math.toRadians(-player.getYaw() % 360);
        var dx = Math.sin(directionInRadians);
        var dz = Math.cos(directionInRadians);

        // 从起点开始
        var x = player.getPos().getX();
        var z = player.getPos().getZ();
        var lastX = (int) x;
        var lastZ = (int) z;

        for (int i = 0; i < 5; i++) {
            // 计算当前所在格子的坐标
            var gridX = (int) x;
            var gridZ = (int) z;
            if (gridX != lastX || gridZ != lastZ) {
                int firstBlockPosY = getFirstBlockPosY(
                        world, gridX, y, gridZ, Math.max(i, 1)
                );
                if (firstBlockPosY != Integer.MIN_VALUE) {
                    if (i == 0 || i == 1) {
                        if (y - firstBlockPosY == 0) {
                            callbackInfoReturnable.setReturnValue(false);
                            return;
                        }
                    } else if (y - firstBlockPosY < i) {
                        callbackInfoReturnable.setReturnValue(false);
                        return;
                    }
                }
                lastX = gridX;
                lastZ = gridZ;
            }
            // 更新位置
            x += dx;
            z += dz;
        }
    }

    /**
     * 获取指定坐标下方的第一个非空气方块的 Y 坐标
     */
    private static int getFirstBlockPosY(World world, int x, int y, int z, int limit) {
        var pos = new BlockPos.Mutable(x, y, z);
        for (int i = 0; i < limit; i++) {
            var posY = pos.getY();
            if (!world.getBlockState(pos).isAir()) return posY;
            pos.setY(posY - 1);
        }
        return Integer.MIN_VALUE;
    }

}