package org.bsdevelopment.pluginutils.particle;

import org.jetbrains.annotations.Nullable;

/**
 * Detects and caches whether certain particle data classes exist on the current server.
 * <p>
 * This avoids NoClassDefFoundError on older Spigot versions by using reflection instead of
 * directly referencing classes like Particle.DustTransition or Particle.Trail.
 */
public final class ParticleDataCapabilities {

    private static final ParticleDataCapabilities INSTANCE = new ParticleDataCapabilities();

    private final Class<?> dustOptionsClass;
    private final Class<?> dustTransitionClass;
    private final Class<?> vibrationClass;
    private final Class<?> vibrationDestinationInterface;
    private final Class<?> vibrationBlockDestinationClass;
    private final Class<?> trailClass;
    private final @Nullable Class<?> spellClass;


    private ParticleDataCapabilities() {
        this.dustOptionsClass = resolve("org.bukkit.Particle$DustOptions");
        this.dustTransitionClass = resolve("org.bukkit.Particle$DustTransition");
        this.vibrationClass = resolve("org.bukkit.Vibration");
        this.vibrationDestinationInterface = resolve("org.bukkit.Vibration$Destination");
        this.vibrationBlockDestinationClass = resolve("org.bukkit.Vibration$Destination$BlockDestination");
        this.trailClass = resolve("org.bukkit.Particle$Trail");
        this.spellClass = resolve("org.bukkit.Particle$Spell");

    }

    private static @Nullable Class<?> resolve(String string) {
        try {
            return Class.forName(string);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    public static ParticleDataCapabilities get() {
        return INSTANCE;
    }

    // --- Spell ---
    public @Nullable Class<?> spellClass() {
        return spellClass;
    }

    public boolean hasSpell() {
        return spellClass != null;
    }

    // --- DustOptions / DustTransition ---

    public boolean hasDustOptions() {
        return dustOptionsClass != null;
    }

    public boolean hasDustTransition() {
        return dustTransitionClass != null;
    }

    public @Nullable Class<?> dustOptionsClass() {
        return dustOptionsClass;
    }

    public @Nullable Class<?> dustTransitionClass() {
        return dustTransitionClass;
    }

    // --- Vibration ---

    public boolean hasVibration() {
        return vibrationClass != null && vibrationDestinationInterface != null;
    }

    public @Nullable Class<?> vibrationClass() {
        return vibrationClass;
    }

    public @Nullable Class<?> vibrationDestinationInterface() {
        return vibrationDestinationInterface;
    }

    public @Nullable Class<?> vibrationBlockDestinationClass() {
        return vibrationBlockDestinationClass;
    }

    // --- Trail ---

    public boolean hasTrail() {
        return trailClass != null;
    }

    public @Nullable Class<?> trailClass() {
        return trailClass;
    }
}
