package com.nationwar;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class CoreListener implements Listener {
    private final NationWarPlugin plugin;
    private final CoreManager coreManager;
    private final NationManager nationManager;
    private final Map<String, BossBar> coreBossBars;
    private final Map<Player, Long> lastHitTime;

    public CoreListener(NationWarPlugin plugin, CoreManager coreManager, NationManager nationManager) {
        this.plugin = plugin;
        this.coreManager = coreManager;
        this.nationManager = nationManager;
        this.coreBossBars = new HashMap<>();
        this.lastHitTime = new HashMap<>();
    }

    // 왼클릭으로 블록을 때릴 때 (주요 이벤트)
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Core core = coreManager.getCoreByBlock(block);
        if (core == null) {
            return;
        }

        Player player = event.getPlayer();
        
        // 전쟁이 활성화되지 않았으면 손상 불가
        if (!coreManager.isWarActive()) {
            event.setCancelled(true);
            player.sendMessage(colorize(plugin.getPrefix() + "&c전쟁이 시작되지 않았습니다!"));
            return;
        }

        // 크리에이티브 모드는 차단
        if (player.getGameMode() == GameMode.CREATIVE) {
            event.setCancelled(true);
            player.sendMessage(colorize(plugin.getPrefix() + "&c크리에이티브 모드에서는 신상을 공격할 수 없습니다!"));
            return;
        }

        // 자기 국가 신상은 공격 불가
        Nation playerNation = nationManager.getPlayerNation(player);
        if (playerNation != null && core.getOwnerNation() != null) {
            if (core.getOwnerNation().contains(playerNation.getName())) {
                event.setCancelled(true);
                player.sendMessage(colorize(plugin.getPrefix() + "&c자신의 국가 신상은 공격할 수 없습니다!"));
                return;
            }
        }

        // 쿨다운 체크 (채팅 스팸 방지)
        long currentTime = System.currentTimeMillis();
        Long lastHit = lastHitTime.get(player);
        boolean showMessage = lastHit == null || (currentTime - lastHit) > 1000; // 1초마다만 메시지

        // 신상에 데미지 입히기 (줄인 데미지)
        double damage = plugin.getConfig().getDouble("core.damage-per-hit", 50.0);
        core.damage(damage);

        // 보스바 업데이트
        updateBossBar(core, player);

        // 타이틀 표시 (채팅 대신)
        showDamageTitle(player, core, damage);

        // 일정 시간마다만 채팅 메시지
        if (showMessage) {
            String damageMsg = colorize(plugin.getConfig().getString("messages.core-damaged", "")
                .replace("{core}", core.getName())
                .replace("{health}", String.format("%.0f", core.getHealth()))
                .replace("{max-health}", String.format("%.0f", core.getMaxHealth())));
            
            player.sendMessage(colorize(plugin.getPrefix()) + damageMsg);
            lastHitTime.put(player, currentTime);
        }

        // 신상이 파괴되었는지 확인
        if (core.isDestroyed()) {
            handleCoreDestruction(core, player);
        }

        event.setCancelled(true);
    }

    private void updateBossBar(Core core, Player player) {
        String coreKey = core.getName();
        BossBar bossBar = coreBossBars.get(coreKey);

        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(
                colorize("&6" + core.getName() + " &f신상"),
                BarColor.RED,
                BarStyle.SOLID
            );
            coreBossBars.put(coreKey, bossBar);
        }

        // 보스바 업데이트
        double healthPercent = core.getHealth() / core.getMaxHealth();
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, healthPercent)));
        bossBar.setTitle(colorize(String.format("&6%s &f신상 &c❤ &f%.0f/%.0f", 
            core.getName(), core.getHealth(), core.getMaxHealth())));

        // 체력에 따라 색상 변경
        if (healthPercent > 0.6) {
            bossBar.setColor(BarColor.GREEN);
        } else if (healthPercent > 0.3) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.RED);
        }

        // 플레이어에게 보스바 표시
        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }

        // 5초 후 자동으로 숨기기
        BossBar finalBossBar = bossBar;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalBossBar.getPlayers().contains(player)) {
                    finalBossBar.removePlayer(player);
                }
            }
        }.runTaskLater(plugin, 100L); // 5초
    }

    private void showDamageTitle(Player player, Core core, double damage) {
        double healthPercent = (core.getHealth() / core.getMaxHealth()) * 100;
        
        String title = colorize("&c-" + (int)damage);
        String subtitle = colorize(String.format("&6%s &7[&e%.1f%%&7]", core.getName(), healthPercent));
        
        player.sendTitle(title, subtitle, 5, 20, 10);
    }

    private String colorize(String message) {
        return message.replace("&", "§");
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        Core core = coreManager.getCoreByBlock(block);
        
        if (core != null) {
            // 블록이 부서지는 것을 방지
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Core core = coreManager.getCoreByBlock(block);
        
        if (core != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getPrefix() + "§c신상 블록은 파괴할 수 없습니다!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        
        // 신상 근처에 블록 설치 방지
        for (Core core : coreManager.getAllCores()) {
            if (core.isInCoreArea(block.getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getPrefix() + "§c신상 근처에는 블록을 설치할 수 없습니다!");
                return;
            }
        }
    }

    private void handleCoreDestruction(Core core, Player destroyer) {
        Nation playerNation = nationManager.getPlayerNation(destroyer);
        
        // 보스바 제거
        BossBar bossBar = coreBossBars.get(core.getName());
        if (bossBar != null) {
            bossBar.removeAll();
            coreBossBars.remove(core.getName());
        }
        
        // 파괴 메시지
        String destroyMsg = colorize(plugin.getConfig().getString("messages.core-destroyed", "")
            .replace("{core}", core.getName()));
        Bukkit.broadcastMessage(colorize(plugin.getPrefix()) + destroyMsg);

        // 파괴 타이틀 (모든 플레이어에게)
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendTitle(
                colorize("&c&l신상 파괴!"),
                colorize("&e" + core.getName() + " &7신상이 점령되었습니다!"),
                10, 60, 20
            );
        }

        // 이전 소유 국가에서 신상 제거
        if (core.getOwnerNation() != null) {
            for (Nation nation : nationManager.getAllNations()) {
                nation.removeOwnedCore(core.getName());
            }
        }

        // 점령 처리
        String nationName;
        if (playerNation != null) {
            nationName = playerNation.getColoredName();
            core.setOwnerNation(playerNation.getName());
            playerNation.addOwnedCore(core.getName());
            playerNation.addPoints(1000); // 포인트 지급
        } else {
            nationName = colorize("&7" + destroyer.getName() + " (무소속)");
            core.setOwnerNation(destroyer.getName());
        }
        
        String captureMsg = colorize(plugin.getConfig().getString("messages.core-captured", "")
            .replace("{nation}", nationName)
            .replace("{core}", core.getName()));
        Bukkit.broadcastMessage(colorize(plugin.getPrefix()) + captureMsg);

        // 신상 체력 복구
        core.setHealth(core.getMaxHealth());
        
        // 데이터 저장
        coreManager.saveCores();
        nationManager.saveNations();

        // 보상 지급
        destroyer.sendMessage(colorize(plugin.getPrefix() + "&a신상을 점령했습니다!"));
        if (playerNation != null) {
            destroyer.sendMessage(colorize(plugin.getPrefix() + "&e국가 포인트 +1000"));
        }
    }
}
