package com.dancedog.flipplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_9_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by DanceDog / Ben
 * on 9/25/19 @ 6:06 PM
 */
public class FlippedPlayerManager implements Listener {

    private HashMap<UUID, FlippedPlayer> flippedPlayers;

    public static class FlippedPlayer {

        private UUID uuid;

        private GameProfile profile;
        private GameProfile flippedProfile;

        /**
         * Stores necessary data that makes the flipping process reversible
         * @param uuid The player's UUID
         * @param profile The player's actual GameProfile
         * @param flippedProfile The new GameProfile to give the player
         */
        public FlippedPlayer(UUID uuid, GameProfile profile, GameProfile flippedProfile) {
            this.uuid = uuid;
            this.profile = profile;
            this.flippedProfile = flippedProfile;
        }

        public UUID getUuid() {
            return uuid;
        }

        public GameProfile getProfile() {
            return profile;
        }

        public GameProfile getFlippedProfile() {
            return flippedProfile;
        }
    }

    /**
     * The username to change profile to
     */
    public static final String FLIP_NAME = "Dinnerbone";

    /**
     * A manager for flipping player models upside down
     */
    public FlippedPlayerManager() {
        this.flippedPlayers = new HashMap<>();
    }

    /**
     * Removes the player from flippedPlayers upon leaving to keep memory consumption down
     * @param event Called when a client disconnects from the server
     */
    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (getPlayer(event.getPlayer().getUniqueId()) != null) flippedPlayers.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Get the flipped player object representing a player
     * @param uuid The UUID of the player
     * @return The FlippedPlayer with that UUID
     */
    public FlippedPlayer getPlayer(UUID uuid) {
        return flippedPlayers.get(uuid);
    }

    /**
     * Get the flipped player object representing a player
     * @param username The username of the player
     * @return The FlippedPlayer with that username
     */
    public FlippedPlayer getPlayerByUsername(String username) {
        for (FlippedPlayer flippedPlayer : flippedPlayers.values()) {
            if (flippedPlayer.getProfile().getName().toLowerCase().equals(username.toLowerCase())) return flippedPlayer;
        }
        return null;
    }

    /**
     * Update a player's GameProfile to "Dinnerbone" in order to flip their model upside down on the client end
     * @param player The player to flip
     */
    public void flipPlayer(final Player player, boolean bypassRemoval) {
        boolean isUpright = (getPlayer(player.getUniqueId()) == null);
        CraftPlayer craftPlayer = (CraftPlayer) player;

        // Keep the player's real username for later
        String realName = "";
        GameProfile profile;

        // If the player needs to be flipped, convert their profile to a flipped one and add them to the flipped list
        if (isUpright) {
            realName = player.getDisplayName();

            profile = new GameProfile(player.getUniqueId(), FLIP_NAME);
            profile.getProperties().put("textures", (Property) craftPlayer.getProfile().getProperties().get("textures").toArray()[0]);

            flippedPlayers.put(player.getUniqueId(), new FlippedPlayer(player.getUniqueId(), craftPlayer.getProfile(), profile));
        }

        // Set the player's GameProfile
        setGameProfile(player.getUniqueId(), craftPlayer.getHandle(), isUpright);

        // Fix chat and tab name when flipped
        if (isUpright) {
            player.setDisplayName(realName);
            player.setPlayerListName(realName);
        }

        // Send following packets to everyone except the target player (would cause freecam)
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {

                // Disconnect and reconnect player on client end so that profile changes take effect
                ((CraftPlayer) online).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(player.getEntityId()));
                ((CraftPlayer) online).getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(craftPlayer.getHandle()));

                // Hide & show the player to reset their model client side
                online.hidePlayer(player);
                online.showPlayer(player);

                // Revert the player's gameprofile so things like tabcomplete & leave messages use correct name
                setGameProfile(player.getUniqueId(), craftPlayer.getHandle(), false);

                // Remove player from flipped list
                if (!bypassRemoval && !isUpright) flippedPlayers.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Set the GameProfile of a flipped player on their handle class
     * @param uuid The UUID of the target player
     * @param entity The target player's handle
     * @param flip true to set to the flipped profile, false to reset their profile
     */
    private void setGameProfile(UUID uuid, EntityPlayer entity, boolean flip) {
        try {
            Field profileField = entity.getClass().getSuperclass().getDeclaredField("bS");
            profileField.setAccessible(true);
            if (flip) {
                profileField.set(entity, getPlayer(uuid).getFlippedProfile());
            } else {
                profileField.set(entity, getPlayer(uuid).getProfile());
            }
            profileField.setAccessible(false);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public HashMap<UUID, FlippedPlayer> getFlippedPlayerMap() {
        return flippedPlayers;
    }
}
