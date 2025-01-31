package top.kmar.mc.se.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.kmar.mc.se.SmartElytraClient;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(
            method = "jump()V",
            at = @At("HEAD")
    )
    private void jump(CallbackInfo callbackInfo) {
        //noinspection DataFlowIssue
        var player = (PlayerEntity) (Object) this;
        SmartElytraClient.setPlayerJumpTimestamp$smart_elytra_client(player.getWorld().getTime());
    }

}