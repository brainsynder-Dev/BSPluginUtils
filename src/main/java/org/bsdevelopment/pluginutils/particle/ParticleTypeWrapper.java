package org.bsdevelopment.pluginutils.particle;

import org.bsdevelopment.pluginutils.utilities.Lazy;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Lightweight wrapper around Bukkit's {@link Particle} enum.
 * <p>
 * Allows:
 * - Referencing particles by string name (for configs / future versions)
 * - Optional fallback to another particle when primary is missing
 */
public final class ParticleTypeWrapper {
    public static final ParticleTypeWrapper NONE = new ParticleTypeWrapper("CRIT", null);

    private final String bukkitName;
    private final @Nullable ParticleTypeWrapper fallback;
    private final Lazy<Particle> lazyParticle = new Lazy<>() {
        @Override
        protected @Nullable Particle load() {
            try {
                return Particle.valueOf(bukkitName);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    };

    private ParticleTypeWrapper(@NotNull String bukkitName, @Nullable ParticleTypeWrapper fallback) {
        this.bukkitName = Objects.requireNonNull(bukkitName, "bukkitName");
        this.fallback = fallback == NONE ? null : fallback;
    }

    public static @NotNull ParticleTypeWrapper of(@NotNull Particle particle) {
        Objects.requireNonNull(particle, "particle is null");
        return new ParticleTypeWrapper(particle.name(), null);
    }

    public static @NotNull ParticleTypeWrapper named(@NotNull String name) {
        return new ParticleTypeWrapper(name, null);
    }

    public static @NotNull ParticleTypeWrapper named(@NotNull String name, @Nullable ParticleTypeWrapper fallback) {
        return new ParticleTypeWrapper(name, fallback);
    }

    public @NotNull String bukkitName() {
        return bukkitName;
    }

    public @Nullable ParticleTypeWrapper fallback() {
        return fallback;
    }

    public boolean isSupported() {
        if (resolveInternal() != null) return true;
        return fallback != null && fallback.isSupported();
    }

    public @Nullable Particle resolve() {
        Particle particle = resolveInternal();
        if (particle != null) return particle;
        if (fallback != null) return fallback.resolve();
        return null;
    }

    private @Nullable Particle resolveInternal() {
        return lazyParticle.get();
    }

    public void spawn(@NotNull World world, @NotNull Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable Object data, boolean force) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(location, "location");

        if (count <= 0) return;

        Particle particle = resolve();
        if (particle == null) return;

        world.spawnParticle(particle, location, count,
                offsetX, offsetY, offsetZ, extra, data, force);
    }

    @Override
    public String toString() {
        return "ParticleHandle{bukkitName='" + bukkitName + '\'' + (fallback != null ? ", fallback=" + fallback.bukkitName : "") + '}';
    }
}
