package be.sazeChat.sazeChat;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;

public class SazeCommand implements CommandExecutor {

    private Map<String, List<Player>> channels = new HashMap<>();
    private final SazeChat plugin;
    private String currentLanguage;
    private List<String> bannedWords;
    private Chat chat;
    private FileConfiguration langConfig;
    private Map<String, String> emojiMap;
    
    public SazeCommand(SazeChat plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    if (args.length == 0) {
        sender.sendMessage("§bSaze Chat V1.0 By Simploe");
        return true;
    }

        Player pl = (Player) sender;
    


        String sub = args[0];

        if (sub.equalsIgnoreCase("reload")) {
            if (sender.hasPermission("sazechat.command.reload")) {
                plugin.reloadConfig(); // Recharge la config.yml
                plugin.loadLanguageConfig(); // Recharge le fichier language.yml
                plugin.loadEmojis(); // Recharge les emojis si nécessaire
                bannedWords = plugin.getConfig().getStringList("bannedWords"); // Recharge les mots bannis

                sender.sendMessage(ChatColor.GREEN + "SazeChat configuration reloaded!");
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            }
            return true;
        }

        if(sub.equalsIgnoreCase("help")) {
            if(sender instanceof Player){
                pl.sendMessage("§x§1§4§8§8§C§C§lS§x§1§7§7§D§C§9§la§x§1§A§7§3§C§6§lz§x§1§D§6§8§C§2§le §x§2§2§5§2§B§C§lC§x§2§5§4§8§B§9§lh§x§2§8§3§D§B§5§la§x§2§B§3§2§B§2§lt");
                pl.sendMessage("§b/sc broadcast <message> : send a broadcast message");
            }
        }



        if(sub.equalsIgnoreCase("broadcast")) {
            if(sender instanceof Player) {
                if (!sender.hasPermission("sazechat.command.broadcast.use")) {
                    pl.sendMessage("§cVous n'avez pas la permission requise !");
                }

                String message = String.join(" ", args).substring(sub.length() + 1);
                message = applyColorCodes(message);

                Bukkit.broadcastMessage("§4§l[Broadcast] §r" + message);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                }


            }
        }
        return true;
    }



    private String applyColorCodes(String message) {
        message = message.replaceAll("(?i)&(#\\w{6})", "§$1");

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
