package com.dancedog.flipplayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Created by DanceDog / Ben
 * on 9/25/19 @ 5:26 PM
 */
public class FlipPlayer extends JavaPlugin {

    private static FlippedPlayerManager flipManager;
    private Team hideTagTeam;

    @Override
    public void onEnable() {
        // Name tag hiding while flipped
        Scoreboard scoreboard = getServer().getScoreboardManager().getMainScoreboard();
        String teamName = "fp.hideTag";
        hideTagTeam = scoreboard.getTeam(teamName);
        if (hideTagTeam == null) {
            hideTagTeam = scoreboard.registerNewTeam(teamName);
            hideTagTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        if (!hideTagTeam.hasEntry(FlippedPlayerManager.FLIP_NAME)) {
            hideTagTeam.addEntry(FlippedPlayerManager.FLIP_NAME);
        }

        // Manages flipped players
        flipManager = new FlippedPlayerManager();

        // Commands
        getCommand("flip").setExecutor(new FlipCommand());
        getCommand("flip").setTabCompleter(new FlipCommand());

        // Events
        this.getServer().getPluginManager().registerEvents(flipManager, this);
    }

    @Override
    public void onDisable() {
        // Fix all players before reload / stop
        flipManager.getFlippedPlayerMap().forEach((uuid, flippedPlayer) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) flipManager.flipPlayer(player, true);
        });

        // Delete tag hider & clear flipManager
        if (hideTagTeam != null) hideTagTeam.unregister();
        flipManager = null;

        super.onDisable();
    }

    static FlippedPlayerManager getFlipManager() {
        return flipManager;
    }
}
