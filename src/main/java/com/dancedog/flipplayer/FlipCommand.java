package com.dancedog.flipplayer;

import com.dancedog.flipplayer.FlippedPlayerManager.FlippedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Created by DanceDog / Ben
 * on 9/25/19 @ 5:30 PM
 */
public class FlipCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandName, String[] args) {
        if (args.length != 1) return false;

        // Search for an already-flipped player
        FlippedPlayer flippedPlayer = FlipPlayer.getFlipManager().getPlayerByUsername(args[0]);
        if (flippedPlayer != null) {
            Player target = Bukkit.getPlayer(flippedPlayer.getUuid());
            FlipPlayer.getFlipManager().flipPlayer(target, false);
            commandSender.sendMessage("Unflipped " + flippedPlayer.getProfile().getName());
            return true;
        }

        // Search for an unflipped player
        for (Player online : Bukkit.getOnlinePlayers()) {
            if(online.getName().equals(args[0])) {
                FlipPlayer.getFlipManager().flipPlayer(online, false);
                commandSender.sendMessage("Flipped " + online.getDisplayName());
                return true;
            }
        }

        // No one found :(
        commandSender.sendMessage("No online player with that name");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
