package org.bsdevelopment.pluginutils.particle;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime particle spawn request.
 * <p>
 * This is built from {@link ParticleConfig} with a concrete world + origin Location.
 */
public record ParticleRequest(@NotNull World world,
        @NotNull Location origin,
        @NotNull ParticleTypeWrapper handle,
        int count,
        double offsetX,
        double offsetY,
        double offsetZ,
        double extra,
        @Nullable ParticlePayload payload,
        boolean force) {
    public void spawn() {
        ParticleDataCapabilities caps = ParticleDataCapabilities.get();
        Object data = payload != null ? payload.toBukkitData(origin, caps) : null;
        handle.spawn(world, origin, count, offsetX, offsetY, offsetZ, extra, data, force);
    }
}
