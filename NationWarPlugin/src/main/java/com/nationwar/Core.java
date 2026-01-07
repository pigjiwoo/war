package com.nationwar;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Beacon;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Core {
    private final String name;
    private final Location centerLocation;
    private final int size;
    private double health;
    private double maxHealth;
    private String ownerNation;
    private boolean isActive;
    private final List<Block> coreBlocks;
    private ArmorStand hologram;
    private Block beaconBlock;
    private final UUID uuid;

    public Core(String name, Location centerLocation, int size, double maxHealth) {
        this.name = name;
        this.centerLocation = centerLocation;
        this.size = size;
        this.health = maxHealth;
        this.maxHealth = maxHealth;
        this.ownerNation = null;
        this.isActive = false;
        this.coreBlocks = new ArrayList<>();
        this.uuid = UUID.randomUUID();
        buildCore();
        createHologram();
    }

    private void buildCore() {
        // 5x5x5 정육면체의 겉면만 생성 (속은 비워둠)
        int halfSize = size / 2;
        Location start = centerLocation.clone().subtract(halfSize, halfSize, halfSize);

        // 겉면만 블록으로 채움
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    // 겉면인지 확인 (최소 하나의 좌표가 0 또는 size-1이어야 함)
                    boolean isOuterLayer = (x == 0 || x == size - 1 ||
                            y == 0 || y == size - 1 ||
                            z == 0 || z == size - 1);

                    if (isOuterLayer) {
                        Location blockLoc = start.clone().add(x, y, z);
                        Block block = blockLoc.getBlock();
                        block.setType(Material.WHITE_CONCRETE);
                        coreBlocks.add(block);
                    }
                }
            }
        }

        // 내부에 신호기 시스템 설치
        setupBeacon();
    }

    private void setupBeacon() {
        // 중심에서 아래로 2칸 내려간 위치 (정육면체 내부 바닥에서 1칸 위)
        Location layer3Center = centerLocation.clone().subtract(0, 1, 0);

        // 3x3 철 블록 설치
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location ironLoc = layer3Center.clone().add(x, 0, z);
                Block ironBlock = ironLoc.getBlock();
                ironBlock.setType(Material.IRON_BLOCK);
            }
        }

        // 신호기 (철 블록 위)
        Location beaconLoc = layer3Center.clone().add(0, 1, 0);
        beaconBlock = beaconLoc.getBlock();
        beaconBlock.setType(Material.BEACON);

        // 유리 (신호기 위)
        Location glassLoc = beaconLoc.clone().add(0, 1, 0);
        Block glassBlock = glassLoc.getBlock();
        glassBlock.setType(Material.WHITE_STAINED_GLASS);

        // 신호기 설정
        if (beaconBlock.getState() instanceof Beacon) {
            Beacon beacon = (Beacon) beaconBlock.getState();
            beacon.update();
        }
    }

    private void createHologram() {
        Location holoLoc = centerLocation.clone().add(0, size/2.0 + 2, 0);
        hologram = (ArmorStand) centerLocation.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCustomNameVisible(true);
        hologram.setMarker(true);
        hologram.setInvulnerable(true);
        updateHologram();
    }

    public void updateHologram() {
        if (hologram != null && !hologram.isDead()) {
            String status = isActive ? "§a[전쟁중]" : "§7[평화]";
            String owner = ownerNation != null ? "§e" + ownerNation : "§7중립";
            
            hologram.setCustomName(String.format("§6§l%s %s\n%s\n§c❤ §f%.0f/%.0f", 
                name, status, owner, health, maxHealth));
        }
    }

    public void damage(double amount) {
        health = Math.max(0, health - amount);
        updateHologram();
    }

    public void heal(double amount) {
        health = Math.min(maxHealth, health + amount);
        updateHologram();
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public boolean isInCoreArea(Location location) {
        int halfSize = size / 2;
        BoundingBox box = new BoundingBox(
            centerLocation.getX() - halfSize - 1,
            centerLocation.getY() - halfSize - 1,
            centerLocation.getZ() - halfSize - 1,
            centerLocation.getX() + halfSize + 2,
            centerLocation.getY() + halfSize + 2,
            centerLocation.getZ() + halfSize + 2
        );
        return box.contains(location.toVector());
    }

    public void remove() {
        // 홀로그램 제거
        if (hologram != null && !hologram.isDead()) {
            hologram.remove();
        }
        
        // 신호기 시스템 제거
        if (beaconBlock != null) {
            // 5층: 유리
            Location glassLoc = beaconBlock.getLocation().clone().add(0, 1, 0);
            glassLoc.getBlock().setType(Material.AIR);
            
            // 4층: 신호기
            beaconBlock.setType(Material.AIR);
            
            // 3층: 3x3 철 블록
            Location ironBase = beaconBlock.getLocation().clone().subtract(0, 1, 0);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location ironLoc = ironBase.clone().add(x, 0, z);
                    ironLoc.getBlock().setType(Material.AIR);
                }
            }
        }
        
        // 모든 콘크리트 블록 제거
        for (Block block : coreBlocks) {
            block.setType(Material.AIR);
        }
        coreBlocks.clear();
    }

    public void setOwnerNation(String nation) {
        this.ownerNation = nation;
        updateHologram();
    }

    public void setActive(boolean active) {
        this.isActive = active;
        // 활성 상태에 따라 블록 색상 변경
        Material material = active ? Material.YELLOW_CONCRETE : Material.WHITE_CONCRETE;
        for (Block block : coreBlocks) {
            block.setType(material);
        }
        updateHologram();
    }

    // Getters
    public String getName() { return name; }
    public Location getCenterLocation() { return centerLocation; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public String getOwnerNation() { return ownerNation; }
    public boolean isActive() { return isActive; }
    public UUID getUuid() { return uuid; }
    public List<Block> getCoreBlocks() { return coreBlocks; }

    // Setters
    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
        updateHologram();
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
        updateHologram();
    }
}
