package com.nationwar;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class Nation {
    private final String name;
    private final UUID uuid;
    private String displayName;
    private ChatColor color;
    private final Set<UUID> members;
    private final Set<UUID> officers;
    private UUID leader;
    private int points;
    private final Set<String> ownedCores;

    public Nation(String name, UUID leader) {
        this.name = name;
        this.uuid = UUID.randomUUID();
        this.displayName = name;
        this.color = ChatColor.WHITE;
        this.members = new HashSet<>();
        this.officers = new HashSet<>();
        this.leader = leader;
        this.members.add(leader);
        this.points = 0;
        this.ownedCores = new HashSet<>();
    }

    // 멤버 관리
    public boolean addMember(UUID playerUuid) {
        return members.add(playerUuid);
    }

    public boolean removeMember(UUID playerUuid) {
        if (playerUuid.equals(leader)) {
            return false; // 리더는 제거 불가
        }
        officers.remove(playerUuid);
        return members.remove(playerUuid);
    }

    public boolean isMember(UUID playerUuid) {
        return members.contains(playerUuid);
    }

    public boolean isMember(Player player) {
        return members.contains(player.getUniqueId());
    }

    // 임원 관리
    public boolean addOfficer(UUID playerUuid) {
        if (!members.contains(playerUuid)) {
            return false;
        }
        return officers.add(playerUuid);
    }

    public boolean removeOfficer(UUID playerUuid) {
        return officers.remove(playerUuid);
    }

    public boolean isOfficer(UUID playerUuid) {
        return officers.contains(playerUuid);
    }

    // 리더 관리
    public void setLeader(UUID newLeader) {
        if (members.contains(newLeader)) {
            this.leader = newLeader;
            officers.add(newLeader);
        }
    }

    public boolean isLeader(UUID playerUuid) {
        return leader.equals(playerUuid);
    }

    public boolean isLeader(Player player) {
        return leader.equals(player.getUniqueId());
    }

    // 권한 확인
    public boolean hasPermission(UUID playerUuid) {
        return isLeader(playerUuid) || isOfficer(playerUuid);
    }

    // 포인트 관리
    public void addPoints(int amount) {
        this.points += amount;
    }

    public void removePoints(int amount) {
        this.points = Math.max(0, this.points - amount);
    }

    // 신상 소유 관리
    public void addOwnedCore(String coreName) {
        ownedCores.add(coreName);
    }

    public void removeOwnedCore(String coreName) {
        ownedCores.remove(coreName);
    }

    // 색상이 적용된 표시 이름
    public String getColoredName() {
        return color + displayName;
    }

    // Getters
    public String getName() { return name; }
    public UUID getUuid() { return uuid; }
    public String getDisplayName() { return displayName; }
    public ChatColor getColor() { return color; }
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public Set<UUID> getOfficers() { return new HashSet<>(officers); }
    public UUID getLeader() { return leader; }
    public int getPoints() { return points; }
    public Set<String> getOwnedCores() { return new HashSet<>(ownedCores); }
    public int getMemberCount() { return members.size(); }

    // Setters
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setColor(ChatColor color) {
        this.color = color;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
