package com.nationwar;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreCommand implements CommandExecutor, TabCompleter {
    private final NationWarPlugin plugin;
    private final CoreManager coreManager;

    public CoreCommand(NationWarPlugin plugin, CoreManager coreManager) {
        this.plugin = plugin;
        this.coreManager = coreManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nationwar.admin")) {
            sender.sendMessage(plugin.getPrefix() + "§c권한이 없습니다!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "생성":
            case "create":
                return handleCreate(sender, args);
            case "제거":
            case "remove":
                return handleRemove(sender, args);
            case "목록":
            case "list":
                return handleList(sender);
            case "시작":
            case "start":
                return handleStart(sender);
            case "종료":
            case "stop":
                return handleStop(sender);
            case "정보":
            case "info":
                return handleInfo(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "§c플레이어만 사용할 수 있습니다!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "§c사용법: /신상 생성 <이름>");
            return true;
        }

        Player player = (Player) sender;
        String name = args[1];

        if (coreManager.getCore(name) != null) {
            sender.sendMessage(plugin.getPrefix() + "§c이미 존재하는 신상 이름입니다!");
            return true;
        }

        Core core = coreManager.createCore(name, player.getLocation());
        
        String msg = plugin.getConfig().getString("messages.core-created", "")
            .replace("{name}", name);
        sender.sendMessage(plugin.getPrefix() + msg);
        
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "§c사용법: /신상 제거 <이름>");
            return true;
        }

        String name = args[1];

        if (coreManager.removeCore(name)) {
            String msg = plugin.getConfig().getString("messages.core-removed", "")
                .replace("{name}", name);
            sender.sendMessage(plugin.getPrefix() + msg);
        } else {
            sender.sendMessage(plugin.getPrefix() + "§c해당 이름의 신상을 찾을 수 없습니다!");
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (coreManager.getAllCores().isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + "§e등록된 신상이 없습니다.");
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + "§6=== 신상 목록 ===");
        for (Core core : coreManager.getAllCores()) {
            String status = core.isActive() ? "§a전쟁중" : "§7평화";
            String owner = core.getOwnerNation() != null ? core.getOwnerNation() : "§7중립";
            sender.sendMessage(String.format("§e• %s §7[%s] §f- 소유: %s, 체력: §c%.0f§f/§c%.0f",
                core.getName(), status, owner, core.getHealth(), core.getMaxHealth()));
        }

        return true;
    }

    private boolean handleStart(CommandSender sender) {
        if (coreManager.isWarActive()) {
            sender.sendMessage(plugin.getPrefix() + "§c이미 전쟁이 진행 중입니다!");
            return true;
        }

        coreManager.startWar();
        String msg = plugin.getConfig().getString("messages.war-started", "");
        plugin.getServer().broadcastMessage(plugin.getPrefix() + msg);

        return true;
    }

    private boolean handleStop(CommandSender sender) {
        if (!coreManager.isWarActive()) {
            sender.sendMessage(plugin.getPrefix() + "§c전쟁이 진행 중이지 않습니다!");
            return true;
        }

        coreManager.endWar();
        String msg = plugin.getConfig().getString("messages.war-ended", "");
        plugin.getServer().broadcastMessage(plugin.getPrefix() + msg);

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + "§c사용법: /신상 정보 <이름>");
            return true;
        }

        String name = args[1];
        Core core = coreManager.getCore(name);

        if (core == null) {
            sender.sendMessage(plugin.getPrefix() + "§c해당 이름의 신상을 찾을 수 없습니다!");
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + "§6=== " + core.getName() + " 정보 ===");
        sender.sendMessage("§e상태: " + (core.isActive() ? "§a전쟁중" : "§7평화"));
        sender.sendMessage("§e소유: " + (core.getOwnerNation() != null ? core.getOwnerNation() : "§7중립"));
        sender.sendMessage(String.format("§e체력: §c%.0f §f/ §c%.0f", core.getHealth(), core.getMaxHealth()));
        sender.sendMessage(String.format("§e위치: §f%s, %.0f, %.0f, %.0f",
            core.getCenterLocation().getWorld().getName(),
            core.getCenterLocation().getX(),
            core.getCenterLocation().getY(),
            core.getCenterLocation().getZ()));

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + "§6=== 신상 명령어 ===");
        sender.sendMessage("§e/신상 생성 <이름> §7- 현재 위치에 신상 생성");
        sender.sendMessage("§e/신상 제거 <이름> §7- 신상 제거");
        sender.sendMessage("§e/신상 목록 §7- 신상 목록 확인");
        sender.sendMessage("§e/신상 시작 §7- 전쟁 시작");
        sender.sendMessage("§e/신상 종료 §7- 전쟁 종료");
        sender.sendMessage("§e/신상 정보 <이름> §7- 신상 정보 확인");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("생성", "제거", "목록", "시작", "종료", "정보"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("제거") || args[0].equalsIgnoreCase("정보") 
                || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("info"))) {
            for (Core core : coreManager.getAllCores()) {
                completions.add(core.getName());
            }
        }

        return completions;
    }
}
