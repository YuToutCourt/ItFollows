package io.github.yutoutcourt.itfollows;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.world.InteractionResult;

public class TestEvents {
    public static void test() {
        EntitySleepEvents.ALLOW_SLEEP_TIME.register((player, pos, vanillaResult) -> {
            return InteractionResult.SUCCESS;
        });
    }
}
