package me.gameisntover.knockbackffa.arena;

import me.gameisntover.knockbackffa.kit.KnockbackFFALegacy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ArenaConfiguration {
    private static File file;

    private static FileConfiguration arenas;

    public static void setup() {
        file = new File(KnockbackFFALegacy.getInstance().getDataFolder(), "zones.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {

            }
        }
        arenas = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get() {
        return arenas;
    }

    public static void save() {
        try {
            arenas.save(file);
        } catch (IOException e) {
            System.out.println("Couldn't save file");
        }
    }

    public static void reload() {
        arenas = YamlConfiguration.loadConfiguration(file);
    }
}
