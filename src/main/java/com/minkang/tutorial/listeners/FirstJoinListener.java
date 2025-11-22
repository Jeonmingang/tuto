package com.minkang.tutorial.listeners;

import com.minkang.tutorial.TutorialWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

/**
 * 첫 접속 시 tutorial.first-join-enabled 가 true 이고
 * config.yml 의 tutorial.first-join-commands 를 실행하고,
 * warp-name 이 설정되어 있으면 해당 워프로 이동시킨다.
 */
public class FirstJoinListener implements Listener {

    private final TutorialWarpPlugin plugin;

    public FirstJoinListener(TutorialWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (!plugin.isFirstJoinEnabled()) return;
        if (!p.hasPlayedBefore()) {
            List<String> cmds = plugin.getConfig().getStringList("tutorial.first-join-commands");
            if (cmds != null && !cmds.isEmpty()) {
                for (String raw : cmds) {
                    String line = raw.trim();
                    if (line.isEmpty()) continue;
                    String replaced = plugin.replace(line, p);

                    if (line.regionMatches(true, 0, "player:", 0, 7)) {
                        p.performCommand(replaced.substring(7).trim());
                    } else if (line.regionMatches(true, 0, "op:", 0, 3)) {
                        boolean was = p.isOp();
                        try {
                            p.setOp(true);
                            p.performCommand(replaced.substring(3).trim());
                        } finally {
                            p.setOp(was);
                        }
                    } else {
                        // 기본은 콘솔 명령어로 간주
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced.replaceFirst("(?i)^console:\s*", ""));
                    }
                }
            }

            // warp-name 이 설정되어 있으면 에센셜 워프로 이동
            String warp = plugin.getWarpName();
            if (warp != null && !warp.isEmpty()) {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warp + " " + p.getName());
                } catch (Exception ex) {
                    plugin.warn("warp-name으로 워프 실행 중 오류: " + ex.getMessage());
                }
            }

            // 튜토리얼 모드 선택 GUI 자동 오픈
            plugin.openStartGui(p);
        }
    }
}
