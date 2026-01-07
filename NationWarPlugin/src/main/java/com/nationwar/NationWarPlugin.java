package com.nationwar;

import org.bukkit.plugin.java.JavaPlugin;

public class NationWarPlugin extends JavaPlugin {
    private CoreManager coreManager;
    private NationManager nationManager;
    private String prefix;

    @Override
    public void onEnable() {
        // Config 로드
        saveDefaultConfig();
        
        // Prefix 설정
        prefix = getConfig().getString("messages.prefix", "§e[§6국가전쟁§e] §f")
            .replace("&", "§");

        // NationManager 초기화
        nationManager = new NationManager(this);

        // CoreManager 초기화
        coreManager = new CoreManager(this);

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new CoreListener(this, coreManager, nationManager), this);

        // 명령어 등록
        CoreCommand coreCommand = new CoreCommand(this, coreManager);
        getCommand("신상").setExecutor(coreCommand);
        getCommand("신상").setTabCompleter(coreCommand);

        NationCommand nationCommand = new NationCommand(this, nationManager);
        getCommand("국가").setExecutor(nationCommand);
        getCommand("국가").setTabCompleter(nationCommand);

        getLogger().info("국가 전쟁 플러그인이 활성화되었습니다!");
        getLogger().info(nationManager.getAllNations().size() + "개의 국가가 로드되었습니다.");
        getLogger().info(coreManager.getAllCores().size() + "개의 신상이 로드되었습니다.");
    }

    @Override
    public void onDisable() {
        if (nationManager != null) {
            nationManager.shutdown();
        }
        if (coreManager != null) {
            coreManager.shutdown();
        }
        getLogger().info("국가 전쟁 플러그인이 비활성화되었습니다!");
    }

    public CoreManager getCoreManager() {
        return coreManager;
    }

    public NationManager getNationManager() {
        return nationManager;
    }

    public String getPrefix() {
        return prefix;
    }
}
