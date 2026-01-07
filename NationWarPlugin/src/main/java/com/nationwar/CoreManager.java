package com.nationwar;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CoreManager {
    private final NationWarPlugin plugin;
    private final Map<String, Core> cores;
    private final Map<Block, Core> blockToCoreMap;
    private boolean warActive;

    public CoreManager(NationWarPlugin plugin) {
        this.plugin = plugin;
        this.cores = new HashMap<>();
        this.blockToCoreMap = new HashMap<>();
        this.warActive = false;
        loadCores();
    }

    public Core createCore(String name, Location location) {
        if (cores.containsKey(name)) {
            return null;
        }

        int size = plugin.getConfig().getInt("core.size", 5);
        double health = plugin.getConfig().getDouble("core.default-health", 10000);
        
        Core core = new Core(name, location, size, health);
        cores.put(name, core);
        
        // 블록 매핑 추가
        for (Block block : core.getCoreBlocks()) {
            blockToCoreMap.put(block, core);
        }
        
        saveCores();
        return core;
    }

    public boolean removeCore(String name) {
        Core core = cores.get(name);
        if (core == null) {
            return false;
        }

        // 블록 매핑 제거
        for (Block block : core.getCoreBlocks()) {
            blockToCoreMap.remove(block);
        }
        
        core.remove();
        cores.remove(name);
        saveCores();
        return true;
    }

    public Core getCore(String name) {
        return cores.get(name);
    }

    public Core getCoreByBlock(Block block) {
        return blockToCoreMap.get(block);
    }

    public Collection<Core> getAllCores() {
        return cores.values();
    }

    public void startWar() {
        warActive = true;
        for (Core core : cores.values()) {
            core.setActive(true);
        }
    }

    public void endWar() {
        warActive = false;
        for (Core core : cores.values()) {
            core.setActive(false);
        }
    }

    public boolean isWarActive() {
        return warActive;
    }

    public void saveCores() {
        File dataFile = new File(plugin.getDataFolder(), "cores.yml");
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        
        data.set("cores", null); // 기존 데이터 초기화
        
        for (Map.Entry<String, Core> entry : cores.entrySet()) {
            String name = entry.getKey();
            Core core = entry.getValue();
            
            String path = "cores." + name + ".";
            data.set(path + "location.world", core.getCenterLocation().getWorld().getName());
            data.set(path + "location.x", core.getCenterLocation().getX());
            data.set(path + "location.y", core.getCenterLocation().getY());
            data.set(path + "location.z", core.getCenterLocation().getZ());
            data.set(path + "health", core.getHealth());
            data.set(path + "max-health", core.getMaxHealth());
            data.set(path + "owner", core.getOwnerNation());
            data.set(path + "active", core.isActive());
        }
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("신상 데이터 저장 실패: " + e.getMessage());
        }
    }

    public void loadCores() {
        File dataFile = new File(plugin.getDataFolder(), "cores.yml");
        if (!dataFile.exists()) {
            return;
        }
        
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        
        if (data.getConfigurationSection("cores") == null) {
            return;
        }
        
        for (String name : data.getConfigurationSection("cores").getKeys(false)) {
            String path = "cores." + name + ".";
            
            String worldName = data.getString(path + "location.world");
            double x = data.getDouble(path + "location.x");
            double y = data.getDouble(path + "location.y");
            double z = data.getDouble(path + "location.z");
            
            Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z);
            
            int size = plugin.getConfig().getInt("core.size", 5);
            double maxHealth = data.getDouble(path + "max-health", 10000);
            
            Core core = new Core(name, location, size, maxHealth);
            core.setHealth(data.getDouble(path + "health"));
            
            String owner = data.getString(path + "owner");
            if (owner != null && !owner.isEmpty()) {
                core.setOwnerNation(owner);
            }
            
            core.setActive(data.getBoolean(path + "active", false));
            
            cores.put(name, core);
            
            // 블록 매핑 추가
            for (Block block : core.getCoreBlocks()) {
                blockToCoreMap.put(block, core);
            }
        }
        
        plugin.getLogger().info(cores.size() + "개의 신상을 로드했습니다.");
    }

    public void shutdown() {
        saveCores();
        for (Core core : cores.values()) {
            core.remove();
        }
        cores.clear();
        blockToCoreMap.clear();
    }
}
