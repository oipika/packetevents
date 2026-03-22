/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.retrooper.packetevents.utils.server;

import io.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;

/**
 * Server Version.
 * This is a nice wrapper over minecraft's protocol versions.
 * You won't have to memorize the protocol version, just memorize the server version you see in the launcher.
 *
 * @author retrooper
 * @see <a href="https://wiki.vg/Protocol_version_numbers">https://wiki.vg/Protocol_version_numbers</a>
 * @since 1.6.9
 */
public enum ServerVersion {
    v_1_7_10(5),
    v_1_8(47), v_1_8_3(47), v_1_8_8(47),
    v_1_9(107), v_1_9_2(109), v_1_9_4(110),
    //1.10 and 1.10.1 are redundant
    v_1_10(210), v_1_10_1(210), v_1_10_2(210),
    v_1_11(315), v_1_11_2(316),
    v_1_12(335), v_1_12_1(338), v_1_12_2(340),
    v_1_13(393), v_1_13_1(401), v_1_13_2(404),
    v_1_14(477), v_1_14_1(480), v_1_14_2(485), v_1_14_3(490), v_1_14_4(498),
    v_1_15(573), v_1_15_1(575), v_1_15_2(578),
    v_1_16(735), v_1_16_1(736), v_1_16_2(751), v_1_16_3(753), v_1_16_4(754), v_1_16_5(754),
    v_1_17(755), v_1_17_1(756),
    v_1_18(757), v_1_18_1(757), v_1_18_2(758),
    ERROR(-1);

    // BUG FIX: kSpigot and other custom forks use unversioned packages
    // (e.g. "org.bukkit.craftbukkit" instead of "org.bukkit.craftbukkit.v1_7_R4")
    // The old code did split(",")[3] which would ArrayIndexOutOfBoundsException on these.
    // We now gracefully handle this by returning empty string for unversioned servers.
    private static final String NMS_VERSION_SUFFIX;

    static {
        String suffix = "";
        try {
            String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
            // Standard Spigot: org.bukkit.craftbukkit.v1_7_R4 -> 4 parts, suffix at [3]
            // kSpigot/Misc:  org.bukkit.craftbukkit -> 3 parts, no suffix
            if (parts.length > 3) {
                suffix = parts[3];
            }
        } catch (Exception e) {
            // If anything goes wrong, leave it empty
        }
        NMS_VERSION_SUFFIX = suffix;
    }

    private static final ServerVersion[] VALUES = values();
    public static ServerVersion[] reversedValues = new ServerVersion[VALUES.length];
    private static ServerVersion cachedVersion;
    private final int protocolVersion;

    ServerVersion(int protocolId) {
        this.protocolVersion = protocolId;
    }

    private static ServerVersion getVersionNoCache() {
        if (reversedValues[0] == null) {
            reversedValues = ServerVersion.reverse();
        }
        for (final ServerVersion val : reversedValues) {
            String valName = val.name().substring(2).replace("_", ".");
            if (Bukkit.getBukkitVersion().contains(valName)) {
                return val;
            }
        }
        ServerVersion fallbackVersion = PacketEvents.get().getSettings().getFallbackServerVersion();
        if (fallbackVersion != null) {
            if (fallbackVersion == ServerVersion.v_1_7_10) {
                try {
                    Class.forName("net.minecraft.util.io.netty.buffer.ByteBuf");
                } catch (Exception ex) {
                    //We will assume its 1.8.8
                    fallbackVersion = ServerVersion.v_1_8_8;
                }
            }
            PacketEvents.get().getPlugin().getLogger().warning("[packetevents] Your server software is preventing us from checking the server version. This is what we found: " + Bukkit.getBukkitVersion() + ". We will assume the server version is " + fallbackVersion.name() + "...");
            return fallbackVersion;
        }
        return ERROR;
    }

    /**
     * Get the server version.
     * If PacketEvents has already attempted resolving, return the cached version.
     * If PacketEvents hasn't already attempted resolving, it will resolve it, cache it and return the version.
     *
     * @return Server Version. (always cached)
     */
    public static ServerVersion getVersion() {
        if (cachedVersion == null) {
            cachedVersion = getVersionNoCache();
        }
        return cachedVersion;
    }

    /**
     * The values in this enum in reverse.
     *
     * @return Reversed server version enum values.
     */
    private static ServerVersion[] reverse() {
        ServerVersion[] array = values();
        int i = 0;
        int j = array.length - 1;
        ServerVersion tmp;
        while (j > i) {
            tmp = array[j];
            array[j--] = array[i];
            array[i++] = tmp;
        }
        return array;
    }

    public static String getNMSSuffix() {
        return NMS_VERSION_SUFFIX;
    }

    /**
     * BUG FIX: For unversioned servers, the NMS directory is just
     * "net.minecraft.server" with no suffix. For standard Spigot, it is
     * "net.minecraft.server.v1_7_R4".
     */
    public static String getNMSDirectory() {
        if (NMS_VERSION_SUFFIX.isEmpty()) {
            return "net.minecraft.server";
        }
        return "net.minecraft.server." + NMS_VERSION_SUFFIX;
    }

    /**
     * BUG FIX: For unversioned servers, the OBC directory is just
     * "org.bukkit.craftbukkit" with no suffix. For standard Spigot, it is
     * "org.bukkit.craftbukkit.v1_7_R4".
     */
    public static String getOBCDirectory() {
        if (NMS_VERSION_SUFFIX.isEmpty()) {
            return "org.bukkit.craftbukkit";
        }
        return "org.bukkit.craftbukkit." + NMS_VERSION_SUFFIX;
    }

    public static ServerVersion getLatest() {
        if (reversedValues[0] == null) {
            reversedValues = ServerVersion.reverse();
        }
        return reversedValues[0];
    }

    public static ServerVersion getOldest() {
        return values()[0];
    }

    /**
     * Get this server version's protocol version.
     *
     * @return Protocol version.
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Is this server version newer than the compared server version?
     * This method simply checks if this server version's protocol version is greater than
     * the compared server version's protocol version.
     *
     * @param target Compared server version.
     * @return Is this server version newer than the compared server version.
     */
    public boolean isNewerThan(ServerVersion target) {
        if (target.protocolVersion != protocolVersion || this == target) {
            return protocolVersion > target.protocolVersion;
        }

        for (ServerVersion version : reversedValues) {
            if (version == target) {
                return false;
            }
            if (version == this) return true;
        }

        return false;
    }

    /**
     * Is this server version older than the compared server version?
     *
     * @param target Compared server version.
     * @return Is this server version older than the compared server version.
     */
    public boolean isOlderThan(ServerVersion target) {
        if (target.protocolVersion != protocolVersion || this == target) {
            return protocolVersion < target.protocolVersion;
        }
        for (ServerVersion version : VALUES) {
            if (version == this) {
                return true;
            } else if (version == target) {
                return false;
            }
        }
        return false;
    }

    public boolean isNewerThanOrEquals(ServerVersion target) {
        return this == target || isNewerThan(target);
    }

    public boolean isOlderThanOrEquals(ServerVersion target) {
        return this == target || isOlderThan(target);
    }

    @Deprecated
    public boolean isHigherThan(final ServerVersion target) {
        return isNewerThan(target);
    }

    @Deprecated
    public boolean isHigherThanOrEquals(final ServerVersion target) {
        return isNewerThanOrEquals(target);
    }

    @Deprecated
    public boolean isLowerThan(final ServerVersion target) {
        return isOlderThan(target);
    }

    @Deprecated
    public boolean isLowerThanOrEquals(final ServerVersion target) {
        return isOlderThanOrEquals(target);
    }
}
