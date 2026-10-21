package org.Cry_Legende.sHPlayerInteract;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class SHPlayerInteract extends JavaPlugin implements Listener {

    private List<String> whitelistedWorlds;
    private List<String> blacklistedRegions;
    private List<String> playerCommands;
    private List<String> consoleCommands;
    private boolean debug;
    private static Permission perms = null;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Load configuration
        loadConfig();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Setup Vault
        if (!setupPermissions()) {
            getLogger().warning("Vault not found! Group permissions will not work properly.");
        } else {
            getLogger().info("Successfully hooked into Vault for permissions!");
        }

        // Log successful plugin start
        getLogger().info("SHPlayerInteract has been enabled!");
    }

    private boolean setupPermissions() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            return false;
        }
        perms = rsp.getProvider();
        return perms != null;
    }

    @Override
    public void onDisable() {
        getLogger().info("SHPlayerInteract has been disabled!");
    }

    private void loadConfig() {
        // Reload config from file
        reloadConfig();
        FileConfiguration config = getConfig();

        // Load whitelisted worlds
        whitelistedWorlds = config.getStringList("whitelist-worlds");
        if (whitelistedWorlds.isEmpty()) {
            getLogger().warning("No worlds are whitelisted! Plugin will not work in any world.");
        }

        // Load blacklisted WorldGuard regions
        blacklistedRegions = config.getStringList("blacklist-regions");

        // Load commands
        playerCommands = config.getStringList("player-commands");
        consoleCommands = config.getStringList("console-commands");

        // Debug mode
        debug = config.getBoolean("debug", false);

        // Log loaded configuration if in debug mode
        if (debug) {
            getLogger().info("Whitelisted worlds: " + String.join(", ", whitelistedWorlds));
            getLogger().info("Blacklisted regions: " + String.join(", ", blacklistedRegions));
            getLogger().info("Player commands: " + String.join(", ", playerCommands));
            getLogger().info("Console commands: " + String.join(", ", consoleCommands));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shplayerinteract")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("shplayerinteract.reload")) {
                    loadConfig();
                    sender.sendMessage(ChatColor.GREEN + "SHPlayerInteract configuration reloaded!");
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "SHPlayerInteract v" + getDescription().getVersion());
                if (sender.hasPermission("shplayerinteract.reload")) {
                    sender.sendMessage(ChatColor.YELLOW + "Use /shplayerinteract reload to reload the configuration.");
                }
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        // Vérifie que l'action se fait avec la main principale pour éviter une double exécution
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Récupérer le joueur qui clique
        Player clicker = event.getPlayer();

        // Vérifie que le joueur est en train de s'accroupir (sneaking)
        if (!clicker.isSneaking()) {
            return;
        }

        // Vérifie que l'entité cliquée est bien un joueur
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }

        // Récupérer le joueur cliqué
        Player clickedPlayer = (Player) event.getRightClicked();

        // Ici, on vérifie que le joueur cliqué figure bien parmi les joueurs en ligne
        // Ainsi, on exclut les NPC qui ne sont pas réels.
        if (!Bukkit.getOnlinePlayers().contains(clickedPlayer)) {
            return;
        }

        // Vérification de la permission avec Vault (ou Bukkit)
        if (!hasPermission(clicker, "shplayerinteract.use")) {
            if (debug) {
                getLogger().info("Le joueur " + clicker.getName() + " ne dispose pas de la permission d'utiliser SHPlayerInteract");
            }
            return;
        }

        // Vérifie que le monde du joueur est en whitelist
        World world = clicker.getWorld();
        if (!whitelistedWorlds.contains(world.getName())) {
            if (debug) {
                getLogger().info("Le monde " + world.getName() + " n'est pas en whitelist");
            }
            return;
        }

        // Vérifie que le joueur n'est pas dans une région blacklistée par WorldGuard
        if (isInBlacklistedRegion(clicker)) {
            if (debug) {
                getLogger().info("Le joueur " + clicker.getName() + " se trouve dans une région blacklistée");
            }
            return;
        }

        // Exécute les commandes définies
        executeCommands(clicker, clickedPlayer);

        // Annule l'interaction (empêche, par exemple, l'ouverture d'un inventaire)
        event.setCancelled(true);
    }


    private boolean isInBlacklistedRegion(Player player) {
        // Check if WorldGuard is available
        Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldGuardPlugin == null) {
            getLogger().warning("WorldGuard not found, region checks disabled");
            return false;
        }

        try {
            // Get the RegionContainer from WorldGuard
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            // Create a query for the player's location
            RegionQuery query = container.createQuery();
            // Get all regions at the player's location
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

            // Check if the player is in any blacklisted region
            for (String blacklistedRegion : blacklistedRegions) {
                if (regions.getRegions().stream().anyMatch(region -> region.getId().equalsIgnoreCase(blacklistedRegion))) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error checking WorldGuard regions", e);
            return false;
        }
    }

    private void executeCommands(Player executor, Player target) {
        // Get the target player's name for placeholder replacement
        String targetName = target.getName();

        // Execute player commands
        for (String cmd : playerCommands) {
            String command = cmd.replace("%player%", targetName);
            if (debug) {
                getLogger().info("Player " + executor.getName() + " executing command: " + command);
            }
            executor.performCommand(command);
        }

        // Execute console commands
        for (String cmd : consoleCommands) {
            String command = cmd.replace("%player%", targetName);
            if (debug) {
                getLogger().info("Console executing command: " + command);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    /**
     * Check if a player has a permission using Vault for group permissions
     * Falls back to Bukkit permissions if Vault is not available
     *
     * @param player     The player to check
     * @param permission The permission to check
     * @return True if the player has the permission
     */
    private boolean hasPermission(Player player, String permission) {
        if (perms != null) {
            // Use Vault to check both player-specific and group permissions
            return perms.has(player, permission);
        } else {
            // Fall back to Bukkit permissions if Vault is not available
            return player.hasPermission(permission);
        }
    }
}
