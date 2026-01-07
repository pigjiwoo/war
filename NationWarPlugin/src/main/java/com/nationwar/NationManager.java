package com.nationwar;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NationManager {
    private final NationWarPlugin plugin;
    private final Map<String, Nation> nations;
    private final Map<UUID, String> playerNations; // 플레이어 UUID -> 국가 이름

    public NationManager(NationWarPlugin plugin) {
        this.plugin = plugin;
        this.nations = new HashMap<>();
        this.playerNations = new HashMap<>();
        loadNations();
    }

    public Nation createNation(String name, Player leader) {
        if (nations.containsKey(name.toLowerCase())) {
            return null;
        }

        if (playerNations.containsKey(leader.getUniqueId())) {
            return null; // 이미 국가에 속해있음
        }

        Nation nation = new Nation(name, leader.getUniqueId());
        nations.put(name.toLowerCase(), nation);
        playerNations.put(leader.getUniqueId(), name.toLowerCase());
        
        saveNations();
        return nation;
    }

    public boolean deleteNation(String name) {
        Nation nation = nations.get(name.toLowerCase());
        if (nation == null) {
            return false;
        }

        // 모든 멤버의 국가 정보 제거
        for (UUID member : nation.getMembers()) {
            playerNations.remove(member);
        }

        nations.remove(name.toLowerCase());
        saveNations();
        return true;
    }

    public Nation getNation(String name) {
        return nations.get(name.toLowerCase());
    }

    public Nation getPlayerNation(Player player) {
        String nationName = playerNations.get(player.getUniqueId());
        if (nationName == null) {
            return null;
        }
        return nations.get(nationName);
    }

    public Nation getPlayerNation(UUID playerUuid) {
        String nationName = playerNations.get(playerUuid);
        if (nationName == null) {
            return null;
        }
        return nations.get(nationName);
    }

    public boolean joinNation(Player player, String nationName) {
        Nation nation = nations.get(nationName.toLowerCase());
        if (nation == null) {
            return false;
        }

        if (playerNations.containsKey(player.getUniqueId())) {
            return false; // 이미 다른 국가에 속해있음
        }

        nation.addMember(player.getUniqueId());
        playerNations.put(player.getUniqueId(), nationName.toLowerCase());
        saveNations();
        return true;
    }

    public boolean leaveNation(Player player) {
        Nation nation = getPlayerNation(player);
        if (nation == null) {
            return false;
        }

        if (nation.isLeader(player)) {
            return false; // 리더는 탈퇴 불가, 국가를 해체해야 함
        }

        nation.removeMember(player.getUniqueId());
        playerNations.remove(player.getUniqueId());
        saveNations();
        return true;
    }

    public boolean kickMember(Nation nation, UUID playerUuid) {
        if (!nation.removeMember(playerUuid)) {
            return false;
        }
        playerNations.remove(playerUuid);
        saveNations();
        return true;
    }

    public Collection<Nation> getAllNations() {
        return nations.values();
    }

    public void saveNations() {
        File dataFile = new File(plugin.getDataFolder(), "nations.yml");
        FileConfiguration data = new YamlConfiguration();

        for (Map.Entry<String, Nation> entry : nations.entrySet()) {
            String name = entry.getKey();
            Nation nation = entry.getValue();

            String path = "nations." + name + ".";
            data.set(path + "displayName", nation.getDisplayName());
            data.set(path + "color", nation.getColor().name());
            data.set(path + "leader", nation.getLeader().toString());
            data.set(path + "points", nation.getPoints());

            List<String> members = new ArrayList<>();
            for (UUID uuid : nation.getMembers()) {
                members.add(uuid.toString());
            }
            data.set(path + "members", members);

            List<String> officers = new ArrayList<>();
            for (UUID uuid : nation.getOfficers()) {
                officers.add(uuid.toString());
            }
            data.set(path + "officers", officers);

            data.set(path + "ownedCores", new ArrayList<>(nation.getOwnedCores()));
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("국가 데이터 저장 실패: " + e.getMessage());
        }
    }

    public void loadNations() {
        File dataFile = new File(plugin.getDataFolder(), "nations.yml");
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        if (data.getConfigurationSection("nations") == null) {
            return;
        }

        for (String name : data.getConfigurationSection("nations").getKeys(false)) {
            String path = "nations." + name + ".";

            String leaderStr = data.getString(path + "leader");
            UUID leader = UUID.fromString(leaderStr);

            Nation nation = new Nation(name, leader);
            nation.setDisplayName(data.getString(path + "displayName", name));
            
            String colorName = data.getString(path + "color", "WHITE");
            try {
                nation.setColor(ChatColor.valueOf(colorName));
            } catch (IllegalArgumentException e) {
                nation.setColor(ChatColor.WHITE);
            }

            nation.setPoints(data.getInt(path + "points", 0));

            // 멤버 로드
            List<String> memberList = data.getStringList(path + "members");
            for (String memberStr : memberList) {
                UUID memberUuid = UUID.fromString(memberStr);
                nation.addMember(memberUuid);
                playerNations.put(memberUuid, name);
            }

            // 임원 로드
            List<String> officerList = data.getStringList(path + "officers");
            for (String officerStr : officerList) {
                UUID officerUuid = UUID.fromString(officerStr);
                nation.addOfficer(officerUuid);
            }

            // 소유 신상 로드
            List<String> ownedCores = data.getStringList(path + "ownedCores");
            for (String coreName : ownedCores) {
                nation.addOwnedCore(coreName);
            }

            nations.put(name, nation);
        }

        plugin.getLogger().info(nations.size() + "개의 국가를 로드했습니다.");
    }

    public void shutdown() {
        saveNations();
        nations.clear();
        playerNations.clear();
    }
}
