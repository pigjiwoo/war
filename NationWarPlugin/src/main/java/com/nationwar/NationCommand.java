package com.nationwar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NationCommand implements CommandExecutor, TabCompleter {
    private final NationWarPlugin plugin;
    private final NationManager nationManager;

    public NationCommand(NationWarPlugin plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "§c플레이어만 사용할 수 있습니다!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
            case "생성":
                return handleCreate(player, args);
            case "delete":
            case "삭제":
                return handleDelete(player, args);
            case "invite":
            case "초대":
                return handleInvite(player, args);
            case "join":
            case "가입":
                return handleJoin(player, args);
            case "leave":
            case "탈퇴":
                return handleLeave(player);
            case "kick":
            case "추방":
                return handleKick(player, args);
            case "info":
            case "정보":
                return handleInfo(player, args);
            case "list":
            case "목록":
                return handleList(player);
            case "officer":
            case "임원":
                return handleOfficer(player, args);
            case "color":
            case "색상":
                return handleColor(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§c사용법: /nation create <국가명>");
            return true;
        }

        if (nationManager.getPlayerNation(player) != null) {
            player.sendMessage(plugin.getPrefix() + "§c이미 국가에 소속되어 있습니다!");
            return true;
        }

        String name = args[1];
        Nation nation = nationManager.createNation(name, player);

        if (nation == null) {
            player.sendMessage(plugin.getPrefix() + "§c이미 존재하는 국가 이름입니다!");
            return true;
        }

        player.sendMessage(plugin.getPrefix() + "§a국가 '" + name + "'을(를) 생성했습니다!");
        Bukkit.broadcastMessage(plugin.getPrefix() + "§e새로운 국가 §6" + name + "§e이(가) 건국되었습니다!");
        
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        Nation nation = nationManager.getPlayerNation(player);
        
        if (nation == null) {
            player.sendMessage(plugin.getPrefix() + "§c국가에 소속되어 있지 않습니다!");
            return true;
        }

        if (!nation.isLeader(player)) {
            player.sendMessage(plugin.getPrefix() + "§c국가 리더만 국가를 삭제할 수 있습니다!");
            return true;
        }

        String nationName = nation.getName();
        nationManager.deleteNation(nationName);
        
        player.sendMessage(plugin.getPrefix() + "§c국가를 해체했습니다.");
        Bukkit.broadcastMessage(plugin.getPrefix() + "§7국가 " + nationName + "이(가) 해체되었습니다.");
        
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§c사용법: /nation invite <플레이어>");
            return true;
        }

        Nation nation = nationManager.getPlayerNation(player);
        if (nation == null) {
            player.sendMessage(plugin.getPrefix() + "§c국가에 소속되어 있지 않습니다!");
            return true;
        }

        if (!nation.hasPermission(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§c권한이 없습니다! (리더 또는 임원만 가능)");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.getPrefix() + "§c해당 플레이어를 찾을 수 없습니다!");
            return true;
        }

        if (nationManager.getPlayerNation(target) != null) {
            player.sendMessage(plugin.getPrefix() + "§c해당 플레이어는 이미 다른 국가에 소속되어 있습니다!");
            return true;
        }

        player.sendMessage(plugin.getPrefix() + "§a" + target.getName() + "님에게 초대를 보냈습니다.");
        target.sendMessage(plugin.getPrefix() + "§e" + nation.getColoredName() + " §e국가에 초대받았습니다!");
        target.sendMessage(plugin.getPrefix() + "§e가입하려면: §f/nation join " + nation.getName());
        
        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§c사용법: /nation join <국가명>");
            return true;
        }

        String nationName = args[1];
        
        if (nationManager.joinNation(player, nationName)) {
            Nation nation = nationManager.getNation(nationName);
            player.sendMessage(plugin.getPrefix() + "§a" + nation.getColoredName() + " §a국가에 가입했습니다!");
            
            // 국가 멤버들에게 알림
            for (UUID memberUuid : nation.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && !member.equals(player)) {
                    member.sendMessage(plugin.getPrefix() + "§e" + player.getName() + "님이 국가에 가입했습니다!");
                }
            }
        } else {
            player.sendMessage(plugin.getPrefix() + "§c국가에 가입할 수 없습니다!");
        }
        
        return true;
    }

    private boolean handleLeave(Player player) {
        if (nationManager.leaveNation(player)) {
            player.sendMessage(plugin.getPrefix() + "§a국가에서 탈퇴했습니다.");
        } else {
            player.sendMessage(plugin.getPrefix() + "§c탈퇴할 수 없습니다! (리더는 국가를 삭제해야 합니다)");
        }
        
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§c사용법: /nation kick <플레이어>");
            return true;
        }

        Nation nation = nationManager.getPlayerNation(player);
        if (nation == null) {
            player.sendMessage(plugin.getPrefix() + "§c국가에 소속되어 있지 않습니다!");
            return true;
        }

        if (!nation.hasPermission(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§c권한이 없습니다!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.getPrefix() + "§c해당 플레이어를 찾을 수 없습니다!");
            return true;
        }

        if (!nation.isMember(target)) {
            player.sendMessage(plugin.getPrefix() + "§c해당 플레이어는 이 국가의 멤버가 아닙니다!");
            return true;
        }

        if (nation.isLeader(target)) {
            player.sendMessage(plugin.getPrefix() + "§c리더는 추방할 수 없습니다!");
            return true;
        }

        nationManager.kickMember(nation, target.getUniqueId());
        player.sendMessage(plugin.getPrefix() + "§a" + target.getName() + "님을 추방했습니다.");
        target.sendMessage(plugin.getPrefix() + "§c국가에서 추방되었습니다.");
        
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        Nation nation;
        
        if (args.length >= 2) {
            nation = nationManager.getNation(args[1]);
            if (nation == null) {
                player.sendMessage(plugin.getPrefix() + "§c해당 국가를 찾을 수 없습니다!");
                return true;
            }
        } else {
            nation = nationManager.getPlayerNation(player);
            if (nation == null) {
                player.sendMessage(plugin.getPrefix() + "§c국가에 소속되어 있지 않습니다!");
                player.sendMessage(plugin.getPrefix() + "§e다른 국가 정보: /nation info <국가명>");
                return true;
            }
        }

        player.sendMessage(plugin.getPrefix() + "§6=== " + nation.getColoredName() + " §6정보 ===");
        
        Player leader = Bukkit.getPlayer(nation.getLeader());
        String leaderName = leader != null ? leader.getName() : "오프라인";
        player.sendMessage("§e리더: §f" + leaderName);
        player.sendMessage("§e멤버 수: §f" + nation.getMemberCount() + "명");
        player.sendMessage("§e포인트: §f" + nation.getPoints());
        player.sendMessage("§e소유 신상: §f" + (nation.getOwnedCores().isEmpty() ? "없음" : String.join(", ", nation.getOwnedCores())));
        
        return true;
    }

    private boolean handleList(Player player) {
        if (nationManager.getAllNations().isEmpty()) {
            player.sendMessage(plugin.getPrefix() + "§e등록된 국가가 없습니다.");
            return true;
        }

        player.sendMessage(plugin.getPrefix() + "§6=== 국가 목록 ===");
        for (Nation nation : nationManager.getAllNations()) {
            player.sendMessage(String.format("§e• %s §7- 멤버: §f%d명, 포인트: §f%d",
                nation.getColoredName(), nation.getMemberCount(), nation.getPoints()));
        }
        
        return true;
    }

    private boolean handleOfficer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getPrefix() + "§c사용법: /nation officer <add|remove> <플레이어>");
            return true;
        }

        Nation nation = nationManager.getPlayerNation(player);
        if (nation == null) {
            player.sendMessage(plugin.getPrefix() + "§c국가에 소속되어 있지 않습니다!");
            return true;
        }

        if (!nation.isLeader(player)) {
            player.sendMessage(plugin.getPrefix() + "§c리더만 임원을 지정할 수 있습니다!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            player.sendMessage(plugin.getPrefix() + "§c해당 플레이어를 찾을 수 없습니다!");
            return true;
        }

        if (!nation.isMember(target)) {
            player.sendMessage(plugin.getPrefix() + "§c해당 플레이어는 이 국가의 멤버가 아닙니다!");
            return true;
        }

        if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("추가")) {
            nation.addOfficer(target.getUniqueId());
            player.sendMessage(plugin.getPrefix() + "§a" + target.getName() + "님을 임원으로 지정했습니다.");
            target.sendMessage(plugin.getPrefix() + "§a임원으로 임명되었습니다!");
        } else if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("제거")) {
            nation.removeOfficer(target.getUniqueId());
            player.sendMessage(plugin.getPrefix() + "§a" + target.getName() + "님의 임원 권한을 해제했습니다.");
            target.sendMessage(plugin.getPrefix() + "§c임원 권한이 해제되었습니다.");
        } else {
            player.sendMessage(plugin.getPrefix() + "§c사용법: /국가 임원 <추가|제거> <플레이어>");
        }

        nationManager.saveNations();
        return true;
    }

    private boolean handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§c사용법: /nation color <색상>");
            player.sendMessage(plugin.getPrefix() + "§e사용 가능한 색상: RED, BLUE, GREEN, YELLOW, PURPLE, AQUA, WHITE, GRAY");
            return true;
        }

        Nation nation = nationManager.getPlayerNation(player);
        if (nation == null) {
            player.sendMessage(plugin.getPrefix() + "§c국가에 소속되어 있지 않습니다!");
            return true;
        }

        if (!nation.isLeader(player)) {
            player.sendMessage(plugin.getPrefix() + "§c리더만 국가 색상을 변경할 수 있습니다!");
            return true;
        }

        try {
            ChatColor color = ChatColor.valueOf(args[1].toUpperCase());
            nation.setColor(color);
            nationManager.saveNations();
            player.sendMessage(plugin.getPrefix() + "§a국가 색상을 변경했습니다: " + nation.getColoredName());
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getPrefix() + "§c올바르지 않은 색상입니다!");
            player.sendMessage(plugin.getPrefix() + "§e사용 가능한 색상: RED, BLUE, GREEN, YELLOW, PURPLE, AQUA, WHITE, GRAY");
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getPrefix() + "§6=== 국가 명령어 ===");
        player.sendMessage("§e/국가 생성 <이름> §7- 국가 생성");
        player.sendMessage("§e/국가 삭제 §7- 국가 해체 (리더만)");
        player.sendMessage("§e/국가 초대 <플레이어> §7- 플레이어 초대");
        player.sendMessage("§e/국가 가입 <국가명> §7- 국가 가입");
        player.sendMessage("§e/국가 탈퇴 §7- 국가 탈퇴");
        player.sendMessage("§e/국가 추방 <플레이어> §7- 멤버 추방");
        player.sendMessage("§e/국가 정보 [국가명] §7- 국가 정보");
        player.sendMessage("§e/국가 목록 §7- 국가 목록");
        player.sendMessage("§e/국가 임원 <추가|제거> <플레이어> §7- 임원 관리");
        player.sendMessage("§e/국가 색상 <색상> §7- 국가 색상 변경");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("생성", "삭제", "초대", "가입", "탈퇴", 
                "추방", "정보", "목록", "임원", "색상"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("가입") || args[0].equalsIgnoreCase("join") 
                    || args[0].equalsIgnoreCase("정보") || args[0].equalsIgnoreCase("info")) {
                for (Nation nation : nationManager.getAllNations()) {
                    completions.add(nation.getName());
                }
            } else if (args[0].equalsIgnoreCase("임원") || args[0].equalsIgnoreCase("officer")) {
                completions.addAll(Arrays.asList("추가", "제거"));
            } else if (args[0].equalsIgnoreCase("색상") || args[0].equalsIgnoreCase("color")) {
                completions.addAll(Arrays.asList("RED", "BLUE", "GREEN", "YELLOW", "PURPLE", "AQUA", "WHITE", "GRAY"));
            }
        }

        return completions;
    }
}
