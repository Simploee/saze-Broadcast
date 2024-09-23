package be.sazeChat.sazeChat;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SazeChat extends JavaPlugin implements Listener {

    private List<String> bannedWords;
    private Chat chat;
    private FileConfiguration langConfig;
    private Map<String, String> emojiMap;
    private String currentLanguage;
    private YamlConfiguration messages;
    private List<String> forbiddenWords;

    @Override
    public void onEnable() {
        LoadConfig();

        // Setup Vault Chat
        if (!setupChat()) {
            System.out.println("Vault or a Chat plugin not found, disabling SazeChat.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("sazechat").setExecutor(new SazeCommand(this));
        getCommand("sazechat").setTabCompleter(new SazeCommandTabCompleter());

        getServer().getPluginManager().registerEvents(this, this);
    }

    void createLanguageFolder() {
        File languageDir = new File(getDataFolder(), "language");
        if (!languageDir.exists()) {
            languageDir.mkdir();
        }

        copyDefaultLanguageFile("en.yml");
        copyDefaultLanguageFile("fr.yml");
        copyDefaultLanguageFile("es.yml");
        copyDefaultLanguageFile("de.yml");
        copyDefaultLanguageFile("it.yml");
    }

    // Méthode pour copier un fichier de langue par défaut
    private void copyDefaultLanguageFile(String fileName) {
        File langFile = new File(getDataFolder() + "/language", fileName);
        if (!langFile.exists()) {
            try {
                langFile.getParentFile().mkdirs();
                langFile.createNewFile();

                try (InputStream in = getResource("language/" + fileName);
                     FileOutputStream out = new FileOutputStream(langFile)) {
                    if (in == null) {
                        getLogger().warning("Default language file not found: " + fileName);
                        return;
                    }
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            } catch (IOException e) {
                getLogger().severe("Could not create language file: " + fileName);
            }
        }
    }

    @Override
    public void onDisable() {
        System.out.println("Saze successfully unloaded!");
    }

    void LoadConfig() {
        saveDefaultConfig();
        loadLanguageConfig();
        createLanguageFolder();
        loadEmojis();

        bannedWords = getConfig().getStringList("banned-words");

        currentLanguage = getConfig().getString("language", "en");  // Défaut à l'anglais
        loadLanguage(currentLanguage);

        loadMessages();
    }

    void loadLanguage(String lang) {
        File langFile = new File(getDataFolder(), "language/" + lang + ".yml");
        if (langFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(langFile);
        } else {
            getLogger().warning("Language file not found for: " + lang + ". Loading default language.");
            messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "language/en.yml")); // Charge l'anglais par défaut
        }
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp == null) {
            return false;
        }
        chat = rsp.getProvider();
        return chat != null;
    }

    void loadLanguageConfig() {
        String lang = getConfig().getString("language", "fr");
        File langFile = new File(getDataFolder() + "/language", lang + ".yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private String getMessage(String key) {
        return applyColorCodes(messages.getString("messages." + key, "Message not found."));
    }

    void loadMessages() {
        String lang = getConfig().getString("language", "fr");
        File langFile = new File(getDataFolder() + "/language", lang + ".yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        this.messages = YamlConfiguration.loadConfiguration(langFile);
        this.forbiddenWords = messages.getStringList("forbidden-words");
    }

    void loadEmojis() {
        FileConfiguration config = getConfig();
        emojiMap = new HashMap<>();

        if (config.contains("emojis")) {
            for (String key : config.getConfigurationSection("emojis").getKeys(false)) {
                emojiMap.put(key, config.getString("emojis." + key));
            }
        } else {
            getLogger().warning("'emojis' section doesn't exist.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(ChatColor.GRAY + getMessage("player-join").replace("%player%", event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        event.setQuitMessage(ChatColor.GRAY + getMessage("player-quit").replace("%player%", event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String prefix = chat != null ? chat.getPlayerPrefix(player) : "";
        String suffix = chat != null ? chat.getPlayerSuffix(player) : "";

        prefix = applyColorCodes(prefix);
        suffix = applyColorCodes(suffix);
        String message = event.getMessage();

        // Banword
        for (String word : bannedWords) {
            if (message.contains(word.toLowerCase())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(getMessage("forbidden-word"));
                return;
            }
        }

        // Ping
        for (Player pl : getServer().getOnlinePlayers()) {
            if (message.contains("@" + pl.getName())) {
                message = message.replace("@" + pl.getName(), ChatColor.YELLOW + "@" + pl.getName() + ChatColor.RESET);
                pl.playSound(pl.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
                pl.sendMessage(ChatColor.YELLOW + getMessage("mention").replace("%player%", player.getName()));
            }
        }

        // Color chat
        if (player.hasPermission("sazechat.color")) {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        // Emoji Replace
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        // Clickable link
        String regex = "(https?:\\/\\/[a-zA-Z0-9\\-.]+\\.[a-zA-Z]{2,}(\\S*)?)";
        if (message.matches(".*" + regex + ".*")) {
            event.setCancelled(true);

            TextComponent finalMessage = new TextComponent(prefix + player.getName() + suffix + ChatColor.GRAY + " §l>> ");

            String[] parts = message.split(" ");
            for (String part : parts) {
                if (part.matches(regex)) {
                    TextComponent link = new TextComponent(part);
                    link.setColor(net.md_5.bungee.api.ChatColor.BLUE);
                    link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, part));
                    link.setUnderlined(true);
                    finalMessage.addExtra(link);
                } else {
                    finalMessage.addExtra(new TextComponent(part + " "));
                }
            }

            for (Player pl : getServer().getOnlinePlayers()) {
                pl.spigot().sendMessage(finalMessage);
            }
        } else {
            event.setFormat(prefix + "%s" + suffix + ChatColor.GRAY + " §l>> " + ChatColor.WHITE + message);
        }
    }

    private String applyColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
