package com.minkang.tutorial.cmd;

import com.minkang.tutorial.TutorialWarpPlugin;
import com.minkang.tutorial.TutorialWarpPlugin.BlockPoint;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class TutorialCommand implements CommandExecutor {

    private final TutorialWarpPlugin plugin;

    public TutorialCommand(TutorialWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }
        Player p = (Player) sender;

        if (!p.hasPermission("tutorial.admin")) {
            p.sendMessage("§c권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(p, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // 영어/한글 별칭을 하나의 내부 명령으로 매핑
        String action;
        switch (sub) {
            case "sethere":
            case "등록":
            case "종료등록":
                action = "sethere"; break;
            case "remove":
            case "삭제":
                action = "remove"; break;
            case "list":
            case "목록":
                action = "list"; break;
            case "clear":
            case "초기화블럭":
            case "블럭초기화":
            case "전체삭제":
                action = "clear"; break;
            case "reload":
            case "리로드":
            case "재로드":
                action = "reload"; break;
            case "test":
            case "테스트":
                action = "test"; break;
            case "reset":
            case "초기화":
                action = "reset"; break;
            case "status":
            case "상태":
                action = "status"; break;
            case "complete":
            case "완료":
                action = "complete"; break;
            case "mode":
            case "모드":
            case "경로":
                action = "mode"; break;
            case "stage-reset":
            case "단계초기화":
                action = "stage-reset"; break;
            case "start":
            case "시작":
                action = "start"; break;
            case "setstage":
            case "단계설정":
                action = "setstage"; break;
            default:
                action = "unknown"; break;
        }

        FileConfiguration cfg = plugin.getConfig();

        switch (action) {
            case "sethere": {
                BlockPoint bp = BlockPoint.fromLocation(p.getLocation().subtract(0, 1, 0));
                if (bp == null || bp.world == null) {
                    p.sendMessage(plugin.prefix() + plugin.color("&c유효한 블럭이 아닙니다."));
                    return true;
                }
                plugin.addBlock(bp);
                String done = cfg.getString("messages.set-done", "&a등록 완료: &f%world% %x%,%y%,%z%");
                done = done.replace("%world%", bp.world)
                        .replace("%x%", String.valueOf(bp.x))
                        .replace("%y%", String.valueOf(bp.y))
                        .replace("%z%", String.valueOf(bp.z));
                p.sendMessage(plugin.prefix() + plugin.color(done));
                return true;
            }
            case "remove": {
                BlockPoint nearest = plugin.findNearestBlock(p.getLocation());
                if (nearest == null) {
                    p.sendMessage(plugin.prefix() + plugin.color(cfg.getString("messages.none", "&7등록된 종료블럭이 없습니다.")));
                    return true;
                }
                plugin.removeBlock(nearest);
                String msg = cfg.getString("messages.removed-nearest", "&c가장 가까운 종료블럭 삭제: &f%world% %x%,%y%,%z%");
                msg = msg.replace("%world%", nearest.world)
                        .replace("%x%", String.valueOf(nearest.x))
                        .replace("%y%", String.valueOf(nearest.y))
                        .replace("%z%", String.valueOf(nearest.z));
                p.sendMessage(plugin.prefix() + plugin.color(msg));
                return true;
            }
            case "list": {
                if (plugin.getBlocks().isEmpty()) {
                    p.sendMessage(plugin.prefix() + plugin.color(cfg.getString("messages.none", "&7등록된 종료블럭이 없습니다.")));
                    return true;
                }
                p.sendMessage(plugin.prefix() + plugin.color(cfg.getString("messages.list-header", "&e[종료블럭 목록]")));
                int i = 1;
                for (BlockPoint bp : plugin.getBlocks()) {
                    String entry = cfg.getString("messages.list-entry", "&7- &f%world% %x%,%y%,%z%");
                    entry = entry.replace("%world%", bp.world)
                            .replace("%x%", String.valueOf(bp.x))
                            .replace("%y%", String.valueOf(bp.y))
                            .replace("%z%", String.valueOf(bp.z))
                            .replace("%index%", String.valueOf(i));
                    p.sendMessage(plugin.color(entry));
                    i++;
                }
                return true;
            }
            case "clear": {
                plugin.clearBlocks();
                p.sendMessage(plugin.prefix() + plugin.color(cfg.getString("messages.cleared", "&c모든 종료블럭을 삭제했습니다.")));
                return true;
            }
            case "reload": {
                plugin.reloadPluginConfig();
                p.sendMessage(plugin.prefix() + plugin.color("&a설정 파일을 다시 불러왔습니다."));
                return true;
            }
            case "test": {
                // on-trigger.actions 강제 실행
                if (args.length >= 2) {
                    Player tp = Bukkit.getPlayerExact(args[1]);
                    if (tp == null) {
                        p.sendMessage("§c온라인 플레이어만 지정 가능합니다.");
                        return true;
                    }
                    plugin.runOnTriggerActions(tp);
                    p.sendMessage(plugin.prefix() + plugin.color("&a지정한 플레이어에게 튜토리얼 완료 액션을 실행했습니다."));
                } else {
                    plugin.runOnTriggerActions(p);
                    p.sendMessage(plugin.prefix() + plugin.color("&a자기 자신에게 튜토리얼 완료 액션을 실행했습니다."));
                }
                return true;
            }
            case "reset": {
                if (args.length < 2) {
                    p.sendMessage(plugin.prefix() + "§e사용법: /" + label + " 초기화 <플레이어|*>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("*")) {
                    plugin.getStore().clearAll();
                    // 온라인 플레이어 보스바 숨기기
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        plugin.hideBossBar(online);
                    }
                    p.sendMessage(plugin.prefix() + plugin.color(cfg.getString("messages.resetall-done", "&c모든 플레이어의 튜토리얼 상태를 초기화했습니다.")));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
                    p.sendMessage("§c존재하지 않는 플레이어입니다.");
                    return true;
                }
                java.util.UUID id = target.getUniqueId();
                if (id == null) {
                    p.sendMessage("§cUUID를 찾을 수 없습니다.");
                    return true;
                }
                plugin.getStore().setCompleted(id, false);
                plugin.getStore().setStage(id, 0);
                plugin.getStore().setMode(id, null);

                Player online = target.getPlayer();
                if (online != null) {
                    plugin.hideBossBar(online);
                }

                String msg = cfg.getString("messages.reset-done", "&a{player}의 튜토리얼 상태를 초기화했습니다.")
                        .replace("{player}", args[1]);
                p.sendMessage(plugin.prefix() + plugin.color(msg));
                return true;
            }
            case "status": {
                if (args.length < 2) {
                    p.sendMessage(plugin.prefix() + "§e사용법: /" + label + " 상태 <플레이어>");
                    return true;
                }
                OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
                if (t == null || (t.getName() == null && !t.hasPlayedBefore())) {
                    p.sendMessage("§c존재하지 않는 플레이어입니다.");
                    return true;
                }
                java.util.UUID id = t.getUniqueId();
                boolean done = plugin.getStore().isCompleted(id);
                int stage = plugin.getStore().getStage(id);
                String mode = plugin.getStore().getMode(id);
                if (mode == null) mode = "full";

                String key = done ? "messages.status-completed" : "messages.status-not-completed";
                String msg = cfg.getString(key, done ? "&a{player}: 완료" : "&e{player}: 미완료")
                        .replace("{player}", args[1]);
                msg += plugin.color("&7 (단계: " + stage + ", 모드: " + mode + ")");
                p.sendMessage(plugin.prefix() + plugin.color(msg));
                return true;
            }
            case "complete": {
                if (args.length < 2) {
                    p.sendMessage(plugin.prefix() + "§e사용법: /" + label + " 완료 <플레이어>");
                    return true;
                }
                Player tp = Bukkit.getPlayerExact(args[1]);
                if (tp == null) {
                    p.sendMessage("§c온라인 플레이어만 지정 가능합니다.");
                    return true;
                }
                plugin.completeTutorial(tp);
                p.sendMessage(plugin.prefix() + plugin.color("&a지정한 플레이어를 튜토리얼 완료 상태로 설정했습니다."));
                return true;
            }
            case "mode": {
                if (args.length < 2) {
                    p.sendMessage(plugin.prefix() + "§e사용법: /" + label + " 모드 <풀|요약|시스템>");
                    return true;
                }
                String v = args[1];
                String mode;
                if (v.equalsIgnoreCase("풀") || v.equalsIgnoreCase("full")) {
                    mode = "full";
                } else if (v.equalsIgnoreCase("요약") || v.equalsIgnoreCase("quick") || v.equalsIgnoreCase("빠른")) {
                    mode = "quick";
                } else if (v.equalsIgnoreCase("시스템") || v.equalsIgnoreCase("system")) {
                    mode = "system";
                } else {
                    p.sendMessage(plugin.prefix() + "§c지원하지 않는 모드입니다. (풀, 요약, 시스템)");
                    return true;
                }
                plugin.setMode(p, mode);
                return true;
            }
            case "stage-reset": {
                plugin.getStore().setStage(p.getUniqueId(), 0);
                plugin.hideBossBar(p);
                p.sendMessage(plugin.prefix() + plugin.color("&a자신의 튜토리얼 단계를 초기화했습니다."));
                return true;
            }
            case "start": {
                openStartGui(p);
                return true;
            }
            case "setstage": {
                if (args.length < 3) {
                    p.sendMessage(plugin.prefix() + "§e사용법: /" + label + " 단계설정 <플레이어> <단계(0이상)>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
                    p.sendMessage("§c존재하지 않는 플레이어입니다.");
                    return true;
                }
                java.util.UUID id = target.getUniqueId();
                if (id == null) {
                    p.sendMessage("§cUUID를 찾을 수 없습니다.");
                    return true;
                }
                int stage;
                try {
                    stage = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    p.sendMessage(plugin.prefix() + "§c단계는 숫자로 입력해야 합니다.");
                    return true;
                }
                if (stage < 0) stage = 0;

                plugin.getStore().setStage(id, stage);

                Player online = target.getPlayer();
                int totalStages = plugin.getTotalStagesForPlayer(id);
                if (online != null) {
                    if (stage == 0) {
                        plugin.hideBossBar(online);
                    } else {
                        if (totalStages <= 0) totalStages = 1;
                        plugin.showStageTitleAndBossbar(online, Math.min(stage, totalStages), totalStages);
                    }
                }

                p.sendMessage(plugin.prefix() + plugin.color("&a" + args[1] + "님의 단계를 " + stage + "로 설정했습니다."));
                return true;
            }
            default:
                sendUsage(p, label);
                return true;
        }
    }

    private void sendUsage(Player p, String label) {
        p.sendMessage(plugin.prefix() + plugin.color("&e사용법: /" + label + " <명령>"));
        p.sendMessage(plugin.color("&7- /" + label + " 종료등록 &f: 현재 위치 발밑 블럭을 종료블럭으로 등록"));
        p.sendMessage(plugin.color("&7- /" + label + " 삭제 &f: 가장 가까운 종료블럭 삭제"));
        p.sendMessage(plugin.color("&7- /" + label + " 목록 &f: 종료블럭 목록 보기"));
        p.sendMessage(plugin.color("&7- /" + label + " 전체삭제 &f: 모든 종료블럭 삭제"));
        p.sendMessage(plugin.color("&7- /" + label + " 리로드 &f: 설정 파일 다시 불러오기"));
        p.sendMessage(plugin.color("&7- /" + label + " 테스트 [플레이어] &f: on-trigger.actions 강제 실행"));
        p.sendMessage(plugin.color("&7- /" + label + " 초기화 <플레이어|*> &f: 튜토리얼 상태/단계/모드 초기화"));
        p.sendMessage(plugin.color("&7- /" + label + " 상태 <플레이어> &f: 튜토리얼 상태 확인"));
        p.sendMessage(plugin.color("&7- /" + label + " 완료 <플레이어> &f: 해당 플레이어를 완료 처리"));
        p.sendMessage(plugin.color("&7- /" + label + " 모드 <풀|요약|시스템> &f: 튜토리얼 분기 선택"));
        p.sendMessage(plugin.color("&7- /" + label + " 시작 &f: 튜토리얼 모드 선택 GUI 열기"));
        p.sendMessage(plugin.color("&7- /" + label + " 단계초기화 &f: 자신의 단계/보스바 초기화"));
        p.sendMessage(plugin.color("&7- /" + label + " 단계설정 <플레이어> <단계> &f: 관리자가 강제로 단계 설정"));
    }

    private void openStartGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, plugin.getModeGuiTitle());

        // 풀 튜토리얼
        ItemStack full = new ItemStack(Material.LIME_WOOL);
        ItemMeta fm = full.getItemMeta();
        fm.setDisplayName(plugin.color("&a풀 튜토리얼"));
        java.util.List<String> loreFull = new ArrayList<>();
        loreFull.add(plugin.color("&7처음부터 끝까지 모든 내용을 안내합니다."));
        fm.setLore(loreFull);
        full.setItemMeta(fm);
        inv.setItem(2, full);

        // 요약 튜토리얼
        ItemStack quick = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta qm = quick.getItemMeta();
        qm.setDisplayName(plugin.color("&e요약 튜토리얼"));
        java.util.List<String> loreQuick = new ArrayList<>();
        loreQuick.add(plugin.color("&7핵심 내용만 빠르게 안내합니다."));
        qm.setLore(loreQuick);
        quick.setItemMeta(qm);
        inv.setItem(4, quick);

        // 시스템 튜토리얼
        ItemStack system = new ItemStack(Material.LIGHT_BLUE_WOOL);
        ItemMeta sm = system.getItemMeta();
        sm.setDisplayName(plugin.color("&b시스템 튜토리얼"));
        java.util.List<String> loreSystem = new ArrayList<>();
        loreSystem.add(plugin.color("&7마을/경제/던전 등 시스템 위주로 안내합니다."));
        sm.setLore(loreSystem);
        system.setItemMeta(sm);
        inv.setItem(6, system);

        p.openInventory(inv);
    }
}
