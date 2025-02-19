package me.gameisntover.knockbackffa.util;

import fr.mrmicky.fastboard.FastBoard;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.gameisntover.knockbackffa.KnockKit;
import me.gameisntover.knockbackffa.arena.Arena;
import me.gameisntover.knockbackffa.arena.ArenaConfiguration;
import me.gameisntover.knockbackffa.arena.ArenaManager;
import me.gameisntover.knockbackffa.arena.VoidChunkGenerator;
import me.gameisntover.knockbackffa.bukkitevents.PlayerJoinArenaEvent;
import me.gameisntover.knockbackffa.configurations.ItemConfiguration;
import me.gameisntover.knockbackffa.configurations.ScoreboardConfiguration;
import me.gameisntover.knockbackffa.cosmetics.Cosmetic;
import me.gameisntover.knockbackffa.cosmetics.TrailCosmetic;
import me.gameisntover.knockbackffa.gui.Items;
import me.gameisntover.knockbackffa.gui.LightGUI;
import me.gameisntover.knockbackffa.kit.KnockbackFFALegacy;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class Knocker {
    public static File playersfolder = new File(KnockbackFFALegacy.getInstance().getDataFolder(), "player data" + File.separator);
    public static Map<UUID, Knocker> knockerMap = new HashMap<>();
    private final UUID uniqueID;
    public File cfile;
    public FileConfiguration config;
    private boolean inGame;
    private boolean inArena;
    private BukkitTask scoreboardTask;
    private Location positionA;
    private Location positionB;
    protected Knocker(UUID uuid) {
        this.uniqueID = uuid;
        cfile = new File(playersfolder, uuid + ".yml");
        if (!cfile.exists()) {
            try {
                cfile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(cfile);
    }

    public static Knocker getKnocker(UUID uuid) {
        if (knockerMap.containsKey(uuid)) return knockerMap.get(uuid);
        else {
            Knocker knocker = new Knocker(uuid);
            knockerMap.put(uuid, knocker);
            return knocker;
        }
    }

    public static Knocker getKnocker(String name) {
        return getKnocker(Bukkit.getPlayer(name).getUniqueId());
    }

    public File getfile() {
        return cfile;
    }

    public FileConfiguration get() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(cfile);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error saving " + cfile.getName() + "!");
        }
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uniqueID);
    }

    public float getBalance() {
        return get().getInt("balance");
    }

    public void setBalance(float balance) {
        get().set("balance", balance);
        saveConfig();
    }

    public void addBalance(float balance) {
        setBalance(getBalance() + balance);
    }

    public void removeBalance(int balance) {
        setBalance(getBalance() - balance);
    }

    public Location getLocation() {
        return getPlayer().getLocation();
    }

    public void playSound(Sound sound, float pitch, float volume) {
        getPlayer().playSound(getLocation(), sound, volume, pitch);
    }

    public void sendMessage(String msg) {
        getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    public void sendMessageWithPrefix(String msg) {
        getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', "&l &eKnockback&6FFA >> &r" + msg));
    }

    public void give(Items item) {
        getPlayer().getInventory().addItem(item.getItem());
    }

    public void openGUI(LightGUI gui) {
        getPlayer().openInventory(gui.getInventory());
    }

    public Cosmetic getSelectedCosmetic() {
        return Cosmetic.getFromString(get().getString("selected-cosmetic"));
    }

    public void loadCosmetic(Cosmetic cosmetic) {
        cosmetic.onLoad(this);
    }

    public void loadTrails(TrailCosmetic cosmetic, PlayerMoveEvent e) {
        cosmetic.setMoveEvent(e);
        cosmetic.onLoad(this);
    }

    public String selectedKit() {
        return get().getString("selected-kit");
    }

    public void leaveCurrentArena() {
        if (ArenaConfiguration.get().getString("mainlobby.world") == null) return;
        double x = ArenaConfiguration.get().getDouble("mainlobby.x");
        double y = ArenaConfiguration.get().getDouble("mainlobby.y");
        double z = ArenaConfiguration.get().getDouble("mainlobby.z");
        World world = Bukkit.getWorld(ArenaConfiguration.get().getString("mainlobby.world"));
        if (world != null) getPlayer().teleport(new Location(world, x, y, z));
        else {
            String worldname = ArenaConfiguration.get().getString("mainlobby.world");
            WorldCreator worldCreator = new WorldCreator(worldname);
            world = worldCreator.createWorld();
            getPlayer().teleport(new Location(world, x, y, z));
            getPlayer().teleport(Bukkit.getWorld("world").getSpawnLocation());
        }
    }

    public void teleportToArena(Arena arena) {
        if (ArenaManager.getEnabledArena().getName().equals(arena.getName())) {
            PlayerJoinArenaEvent event = new PlayerJoinArenaEvent(this, arena);
            Bukkit.getPluginManager().callEvent(event);
            getPlayer().teleport(arena.getSpawnLocation());
        }
    }


    public void teleportPlayerToArena() {
        if (ArenaManager.getfolder().list().length > 0) {
            PlayerJoinArenaEvent event = new PlayerJoinArenaEvent(this, ArenaManager.getEnabledArena());
            Bukkit.getPluginManager().callEvent(event);
            Location spawnLoc = ArenaManager.getEnabledArena().getSpawnLocation();
            if (spawnLoc.getWorld() != null) if (!event.isCancelled()) getPlayer().teleport(spawnLoc);
            else {
                WorldCreator wc = new WorldCreator(ArenaManager.getEnabledArena().getConfig().getString("arena.spawn.world"));
                wc.generateStructures(false);
                wc.generator(new VoidChunkGenerator());
                World world1 = wc.createWorld();
                Bukkit.getWorlds().add(world1);
                world1.loadChunk(0, 0);
                if (!event.isCancelled())
                    getPlayer().teleport(new Location(world1, spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
            }
        } else System.out.println("[KnockbackFFA] There are no arenas to teleport the player there!");

    }

    public void toggleScoreBoard(boolean toggle) {
        if (!toggle) return;
        FastBoard fastBoard = new FastBoard(getPlayer());
        List<String> titles = ScoreboardConfiguration.get().getStringList("title");
        final Integer[] a = {0};
        scoreboardTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (getPlayer() != null) {
                    if (a[0] > titles.size() - 1) a[0] = 0;
                    fastBoard.updateTitle(ChatColor.translateAlternateColorCodes('&', titles.get(a[0])));
                    fastBoard.updateLines(PlaceholderAPI.setPlaceholders(getPlayer(), ScoreboardConfiguration.get().getStringList("lines")));
                    if (getPlayer().getScoreboard() != null) getPlayer().getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
                }
            }
        }.runTaskTimer(KnockbackFFALegacy.getInstance(), 5, 5);
    }

    public void giveKit(KnockKit kit) {
        if (!cfile.exists()) return;
        if (kit.get().isSet("contents")) {
            List<ItemStack> kitContents = kit.getItems();
            getPlayer().getInventory().setContents(kitContents.toArray(new ItemStack[0]));
            for (ItemStack item : kitContents) {
                if (item.getType().name().contains("Helmet")) getPlayer().getInventory().setHelmet(item);
                if (item.getType().name().contains("Chestplate")) getPlayer().getInventory().setChestplate(item);
                if (item.getType().name().contains("Leggings")) getPlayer().getInventory().setLeggings(item);
                if (item.getType().name().contains("Boots")) getPlayer().getInventory().setBoots(item);
            }
        } else {
            get().set("owned-kits", get().getStringList("owned-kits").stream().filter(s -> s.contains(kit.getName())).collect(Collectors.toList()));
            saveConfig();
        }
    }

    public boolean isInMainLobby() {
        if (ArenaConfiguration.get().isSet("mainlobby.world"))
            return getLocation().getWorld().equals(Bukkit.getWorld(ArenaConfiguration.get().getString("mainlobby.world")));
        else return false;
    }

    public void giveLobbyItems() {
        getInventory().setItem(ItemConfiguration.get().getInt("LobbyItems.cosmetic.slot"), Items.COSMETICS_MENU.getItem());
        getInventory().setItem(ItemConfiguration.get().getInt("LobbyItems.kits.slot"), Items.KITS_MENU.getItem());
        getInventory().setItem(ItemConfiguration.get().getInt("LobbyItems.shop.slot"), Items.SHOP_MENU.getItem());
    }

    public Inventory getInventory() {
        return getPlayer().getInventory();
    }
}
