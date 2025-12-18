package com.shaolian.manhunt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameCommandExecutor implements CommandExecutor {
    private final GameListener gameListener;

    public GameCommandExecutor(GameListener gameListener) {
        this.gameListener = gameListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gotoend")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // 调用 GameListener 中的逻辑
                return gameListener.handleGotoEndCommand(player);
            }
        }
        return false;
    }
}
