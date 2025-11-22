package com.minkang.tutorial;

import com.minkang.tutorial.cmd.TutorialCommand;
import com.minkang.tutorial.listeners.FirstJoinListener;
import com.minkang.tutorial.listeners.MoveListener;
import com.minkang.tutorial.listeners.TutorialGuiListener;
import com.minkang.tutorial.store.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 튜토리얼 메인 플러그인.
 *
 * - 종료블럭 / 반경 트리거
 * - 단계 시스템
 * - 보스바 진행도 / 타이틀 안내
 * - 네비게이션 파티클
 * - 튜토리얼 모드(분기) 관리
 */
public class TutorialWarpPlugin extends JavaPlugin {

    // config
    private boolean debug;
    private boolean firstJoinEnabled;
    private String warpName;
    private boolean oncePerPlayer;
    private String triggerMode;
    private double radius;
    private boolean requireSneak;

    // 종료 블럭 목록 (등록 순서가 단계의 기본 순서)
    private final LinkedHashSet<BlockPoint> blocks = new LinkedHashSet<>();

    // 데이터 저장
    private DataStore store;

    // 플레이어별 보스바
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    // 네비게이션 파티클 태스크 ID
    private int particleTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPluginConfig();

        // data store
        store = new DataStore(new java.io.File(getDataFolder(), "tutorial-data.yml"));

        // 리스너 / 커맨드 등록
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new TutorialGuiListener(this), this);
        Objects.requireNonNull(getCommand("tutorial")).setExecutor(new TutorialCommand(this));

        // 네비게이션 파티클 반복 태스크
        startParticleTask();

        getLogger().info("TutorialWarp enabled.");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
        stopParticleTask();
        // 보스바 정리
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
        getLogger().info("TutorialWarp disabled.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        FileConfiguration cfg = getConfig();
        debug = cfg.getBoolean("debug", false);

        firstJoinEnabled = cfg.getBoolean("tutorial.first-join-enabled", true);
        warpName = cfg.getString("tutorial.warp-name", "tutorial");
        oncePerPlayer = cfg.getBoolean("tutorial.once-per-player", true);

        triggerMode = cfg.getString("trigger.mode", "block").toLowerCase(Locale.ROOT);
        radius = cfg.getDouble("trigger.radius", 1.2);
        requireSneak = cfg.getBoolean("trigger.require-sneak", false);

        blocks.clear();
        if (cfg.isList("trigger.blocks")) {
            for (Object o : cfg.getList("trigger.blocks")) {
                if (o == null) continue;
                String s = String.valueOf(o);
                BlockPoint bp = BlockPoint.parse(s);
                if (bp != null) blocks.add(bp);
            }
        }
    }

    // --- 기본 getter / helper ---

    public boolean isDebug() { return debug; }

    public boolean isFirstJoinEnabled() { return firstJoinEnabled; }

    public String getWarpName() { return warpName; }

    public boolean isOncePerPlayer() { return oncePerPlayer; }

    public boolean isRequireSneak() { return requireSneak; }

    public Set<BlockPoint> getBlocks() { return Collections.unmodifiableSet(blocks); }

    public DataStore getStore() { return store; }

    public String prefix() {
        return color(getConfig().getString("messages.prefix", "&6[튜토리얼]&r "));
    }

    public String getModeGuiTitle() {
        return color("&8튜토리얼 선택");
    }

    /**
     * 튜토리얼 모드 선택 GUI 열기 (풀/요약/시스템).
     * 첫 접속 시 자동 오픈하거나, /튜토리얼 시작 명령어에서 호출할 수 있다.
     */
    public void openStartGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, getModeGuiTitle());

        // 풀 튜토리얼
        ItemStack full = new ItemStack(Material.LIME_WOOL);
        ItemMeta fm = full.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(color("&a풀 튜토리얼"));
            java.util.List<String> loreFull = new java.util.ArrayList<>();
            loreFull.add(color("&7처음부터 끝까지 모든 내용을 안내합니다."));
            fm.setLore(loreFull);
            full.setItemMeta(fm);
        }
        inv.setItem(2, full);

        // 요약 튜토리얼
        ItemStack quick = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta qm = quick.getItemMeta();
        if (qm != null) {
            qm.setDisplayName(color("&e요약 튜토리얼"));
            java.util.List<String> loreQuick = new java.util.ArrayList<>();
            loreQuick.add(color("&7핵심 내용만 빠르게 안내합니다."));
            qm.setLore(loreQuick);
            quick.setItemMeta(qm);
        }
        inv.setItem(4, quick);

        // 시스템 튜토리얼
        ItemStack system = new ItemStack(Material.LIGHT_BLUE_WOOL);
        ItemMeta sm = system.getItemMeta();
        if (sm != null) {
            sm.setDisplayName(color("&b시스템 튜토리얼"));
            java.util.List<String> loreSystem = new java.util.ArrayList<>();
            loreSystem.add(color("&7마을/경제/던전 등 시스템 위주로 안내합니다."));
            sm.setLore(loreSystem);
            system.setItemMeta(sm);
        }
        inv.setItem(6, system);

        p.openInventory(inv);
    }

    public void info(String msg) { getLogger().info(msg); }

    public void warn(String msg) { getLogger().warning(msg); }

    public String color(String s) {
        return s == null ? "" : s.replace("&", "§");
    }

    public String replace(String s, Player p) {
        if (s == null) return "";
        return s
                .replace("{player}", p.getName())
                .replace("%player%", p.getName());
    }

    // --- 종료 블럭 관리 ---

    public void addBlock(BlockPoint bp) {
        blocks.add(bp);
        saveBlocksToConfig();
    }

    public void removeBlock(BlockPoint bp) {
        blocks.remove(bp);
        saveBlocksToConfig();
    }

    public void clearBlocks() {
        blocks.clear();
        saveBlocksToConfig();
    }

    private void saveBlocksToConfig() {
        FileConfiguration cfg = getConfig();
        java.util.List<String> list = blocks.stream().map(BlockPoint::toString).collect(Collectors.toList());
        cfg.set("trigger.blocks", list);
        saveConfig();
    }

    /**
     * 플레이어 위치/웅크림 여부로 트리거 진입 여부 판정
     */
    public boolean isTriggered(Location to, boolean sneaking) {
        if (to == null) return false;
        if (requireSneak && !sneaking) return false;

        if ("radius".equalsIgnoreCase(triggerMode)) {
            // 중심: 모든 종료 블럭의 평균 위치
            if (blocks.isEmpty()) return false;
            double cx = 0, cy = 0, cz = 0;
            World w = null;
            int count = 0;
            for (BlockPoint bp : blocks) {
                Location l = bp.toLocation();
                if (l == null) continue;
                if (w == null) w = l.getWorld();
                if (!Objects.equals(w, l.getWorld())) continue;
                cx += l.getX();
                cy += l.getY();
                cz += l.getZ();
                count++;
            }
            if (count == 0 || w == null) return false;
            cx /= count;
            cy /= count;
            cz /= count;
            if (!Objects.equals(w, to.getWorld())) return false;
            Location center = new Location(w, cx, cy, cz);
            return center.distanceSquared(to) <= radius * radius;
        } else { // block 모드
            BlockPoint bp = BlockPoint.fromLocation(to);
            return blocks.contains(bp);
        }
    }

    public BlockPoint findNearestBlock(Location loc) {
        if (loc == null || blocks.isEmpty()) return null;
        BlockPoint best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPoint bp : blocks) {
            Location l = bp.toLocation();
            if (l == null || !Objects.equals(l.getWorld(), loc.getWorld())) continue;
            double d = l.distanceSquared(loc);
            if (d < bestDist) {
                bestDist = d;
                best = bp;
            }
        }
        return best;
    }

    // --- 블럭 인덱스 / 단계 매핑 ---

    /**
     * blocks(등록 순서)에서의 인덱스(0-based)를 반환.
     */
    public int getBaseIndexForBlock(BlockPoint bp) {
        if (bp == null) return -1;
        int i = 0;
        for (BlockPoint b : blocks) {
            if (b.equals(bp)) return i;
            i++;
        }
        return -1;
    }

    /**
     * 플레이어의 모드에 따라 "어떤 블럭들이 어떤 순서의 단계인지"를 결정.
     * - paths.<mode>.stages: [1,3,...] 처럼 1-based 블럭 번호를 받음.
     * - 잘못된 번호는 무시.
     * - 지정이 없으면 blocks 전체(0..size-1)를 기본 순서로 사용.
     */
    public java.util.List<Integer> getStageMappingForPlayer(UUID uuid) {
        java.util.List<Integer> mapping = new ArrayList<>();
        int size = blocks.size();
        if (size == 0) return mapping;

        FileConfiguration cfg = getConfig();
        String mode = store != null ? store.getMode(uuid) : null;
        if (mode == null || mode.isEmpty()) mode = "full";

        String basePath = "paths." + mode + ".stages";
        if (cfg.isList(basePath)) {
            java.util.List<?> raw = cfg.getList(basePath);
            if (raw != null) {
                for (Object o : raw) {
                    if (o == null) continue;
                    String s = String.valueOf(o).trim();
                    if (s.isEmpty()) continue;
                    try {
                        int stageNumber = Integer.parseInt(s); // 1-based
                        int idx = stageNumber - 1;
                        if (idx >= 0 && idx < size && !mapping.contains(idx)) {
                            mapping.add(idx);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 설정이 없거나 유효한 값이 없다면 기본 전체 순서 사용
        if (mapping.isEmpty()) {
            for (int i = 0; i < size; i++) mapping.add(i);
        }

        return mapping;
    }

    /**
     * 이 플레이어에게서, 특정 블럭이 "몇 번째 단계인지"(0-based)를 반환.
     */
    public int getLogicalStageIndexForBlock(UUID uuid, BlockPoint bp) {
        int baseIndex = getBaseIndexForBlock(bp);
        if (baseIndex < 0) return -1;
        java.util.List<Integer> mapping = getStageMappingForPlayer(uuid);
        for (int i = 0; i < mapping.size(); i++) {
            if (mapping.get(i) == baseIndex) return i;
        }
        return -1;
    }

    /**
     * 이 플레이어에게서, 주어진 단계 인덱스(0-based)가 어떤 블럭인지 반환.
     */
    public BlockPoint getBlockForStageIndex(UUID uuid, int stageIndex) {
        java.util.List<Integer> mapping = getStageMappingForPlayer(uuid);
        if (stageIndex < 0 || stageIndex >= mapping.size()) return null;
        int baseIndex = mapping.get(stageIndex);
        int i = 0;
        for (BlockPoint bp : blocks) {
            if (i == baseIndex) return bp;
            i++;
        }
        return null;
    }

    /**
     * 플레이어의 모드에 따라 사용할 총 단계 수.
     */
    public int getTotalStagesForPlayer(UUID uuid) {
        return getStageMappingForPlayer(uuid).size();
    }

    // --- 튜토리얼 진행 / 단계 시스템 ---

    /**
     * MoveListener에서 호출됨.
     * 종료 블럭 / 반경에 진입했을 때 단계 진행 및 보상 처리.
     */
    public void handleTrigger(Player p) {
        UUID id = p.getUniqueId();

        if (oncePerPlayer && store.isCompleted(id)) {
            if (getConfig().getBoolean("messages.show-already-completed", true)) {
                String msg = getConfig().getString("messages.already-completed", "&7이미 튜토리얼을 완료했습니다.");
                p.sendMessage(prefix() + color(msg));
            }
            return;
        }

        int totalStages = getTotalStagesForPlayer(id);
        if (totalStages <= 1 || blocks.isEmpty()) {
            // 단계가 없으면 기존 방식으로 바로 완료 처리
            completeTutorial(p);
            return;
        }

        // 어느 블럭(단계)에 있는지 계산
        BlockPoint currentBlock = BlockPoint.fromLocation(p.getLocation());
        int logicalIndex = getLogicalStageIndexForBlock(id, currentBlock); // 0-based

        if (logicalIndex < 0) {
            // 반경 모드라면 마지막 단계로 취급 가능
            if ("radius".equalsIgnoreCase(triggerMode)) {
                logicalIndex = totalStages - 1;
            } else {
                return;
            }
        }

        int newStage = logicalIndex + 1;
        int prevStage = store.getStage(id);

        // 뒤로 가는 경우는 무시
        if (newStage <= prevStage) {
            return;
        }

        // 스킵해서 앞으로 간 경우라도 newStage 기준으로 업데이트
        store.setStage(id, newStage);

        // 단계별 액션 실행
        runStageActions(p, newStage);

        // 보스바 / 타이틀 업데이트
        showStageTitleAndBossbar(p, newStage, totalStages);

        // 마지막 단계면 튜토리얼 완료 처리
        if (newStage >= totalStages) {
            completeTutorial(p);
        }
    }

    /**
     * stages.titles.<stage>.actions 에 정의된 액션 실행.
     */
    public void runStageActions(Player p, int stage) {
        FileConfiguration cfg = getConfig();
        String basePath = "stages.titles." + stage + ".actions";
        java.util.List<String> actions = cfg.getStringList(basePath);
        if (actions == null || actions.isEmpty()) return;

        for (String raw : actions) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String replaced = replace(line, p);
            try {
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
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced.replaceFirst("(?i)^console:\\s*", ""));
                }
            } catch (Exception e) {
                getLogger().warning("Failed to execute stage " + stage + " action: " + line);
                e.printStackTrace();
            }
        }
    }

    public void showStageTitleAndBossbar(Player p, int current, int total) {
        FileConfiguration cfg = getConfig();

        // 타이틀
        String path = store.getMode(p.getUniqueId());
        if (path == null) path = "full";

        String titleKey = "stages.titles." + current + ".title";
        String subKey = "stages.titles." + current + ".subtitle";

        String title = cfg.getString(titleKey, cfg.getString("stages.default.title", "&a[튜토리얼 {stage}단계]"));
        String subtitle = cfg.getString(subKey, cfg.getString("stages.default.subtitle", "&7튜토리얼을 진행해주세요."));

        title = title
                .replace("{stage}", String.valueOf(current))
                .replace("{total}", String.valueOf(total))
                .replace("{mode}", path);

        subtitle = subtitle
                .replace("{stage}", String.valueOf(current))
                .replace("{total}", String.valueOf(total))
                .replace("{mode}", path);

        p.sendTitle(color(replace(title, p)), color(replace(subtitle, p)), 10, 40, 10);

        // 보스바
        double progress = Math.max(0.0, Math.min(1.0, (double) current / (double) total));
        BossBar bar = bossBars.computeIfAbsent(p.getUniqueId(), k -> {
            BossBar b = Bukkit.createBossBar(color("&e튜토리얼 진행도"), BarColor.YELLOW, BarStyle.SEGMENTED_10);
            b.addPlayer(p);
            return b;
        });
        String barText = cfg.getString("stages.bossbar-title", "&e튜토리얼 진행도: &f{current}&7/&f{total}");
        barText = barText
                .replace("{current}", String.valueOf(current))
                .replace("{total}", String.valueOf(total));
        bar.setTitle(color(replace(barText, p)));
        bar.setProgress(progress);
        bar.setVisible(true);
    }

    public void hideBossBar(Player p) {
        BossBar bar = bossBars.remove(p.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    /**
     * 튜토리얼 완료 처리:
     * - DataStore에 completed=true
     * - 메시지 전송
     * - on-trigger.actions 실행
     * - 보스바 숨기기
     */
    public void completeTutorial(Player p) {
        UUID id = p.getUniqueId();
        store.setCompleted(id, true);

        String msg = getConfig().getString("messages.completed-now", "&a튜토리얼을 완료했습니다!");
        p.sendMessage(prefix() + color(replace(msg, p)));

        hideBossBar(p);

        // on-trigger.actions 실행
        runOnTriggerActions(p);
    }

    // --- on-trigger.actions 처리 (기존 기능) ---

    public void runOnTriggerActions(Player p) {
        if (debug) info("Trigger matched for " + p.getName());
        List<String> actions = getConfig().getStringList("on-trigger.actions");
        if (actions == null) return;
        for (String raw : actions) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String replaced = replace(line, p);
            try {
                if (line.regionMatches(true, 0, "player:", 0, 7)) {
                    // 플레이어 권한
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
                    // 콘솔
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced.replaceFirst("(?i)^console:\\s*", ""));
                }
            } catch (Exception e) {
                getLogger().warning("Failed to execute on-trigger action: " + line);
                e.printStackTrace();
            }
        }
    }

    // --- 튜토리얼 모드(분기) ---

    public void setMode(Player p, String mode) {
        store.setMode(p.getUniqueId(), mode);
        String key = "paths." + mode + ".name";
        String name = getConfig().getString(key, mode);
        String msg = getConfig().getString("messages.mode-set", "&a튜토리얼 모드가 &f{mode}&a(으)로 설정되었습니다.");
        msg = msg.replace("{mode}", name);
        p.sendMessage(prefix() + color(replace(msg, p)));
    }

    public String getMode(Player p) {
        String mode = store.getMode(p.getUniqueId());
        if (mode == null) {
            return "full";
        }
        return mode;
    }

    // --- 네비게이션 파티클 ---

    private void startParticleTask() {
        if (particleTaskId != -1) return;
        particleTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (blocks.isEmpty()) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();
                if (oncePerPlayer && store.isCompleted(id)) continue;

                int totalStages = getTotalStagesForPlayer(id);
                if (totalStages <= 0) continue;
                int currentStage = store.getStage(id);
                int nextStage = Math.min(totalStages, currentStage + 1);
                if (nextStage <= 0) nextStage = 1;

                // 다음 단계에 해당하는 블럭 위치
                BlockPoint targetBlock = getBlockForStageIndex(id, nextStage - 1);
                if (targetBlock == null) continue;
                Location loc = targetBlock.toLocation();
                if (loc == null) continue;

                if (!Objects.equals(loc.getWorld(), p.getWorld())) continue;
                // 일정 반경 안에 있을 때만 보여주면 과도한 파티클 방지
                if (p.getLocation().distanceSquared(loc) > 64 * 64) continue;

                // 파티클 스폰 (기둥처럼)
                for (int i = 0; i < 6; i++) {
                    Location l = loc.clone().add(0.5, i * 0.5, 0.5);
                    p.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, l, 1, 0, 0, 0, 0);
                }
            }
        }, 40L, 40L); // 2초마다
    }

    private void stopParticleTask() {
        if (particleTaskId != -1) {
            getServer().getScheduler().cancelTask(particleTaskId);
            particleTaskId = -1;
        }
    }

    // --- 내부 static class: BlockPoint ---

    /**
     * 블럭 좌표를 간단히 직렬화/역직렬화하기 위한 클래스.
     * world,x,y,z 형태로 config에 저장됨.
     */
    public static class BlockPoint {
        public final String world;
        public final int x, y, z;

        public BlockPoint(String world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static BlockPoint fromLocation(Location loc) {
            if (loc == null || loc.getWorld() == null) return null;
            return new BlockPoint(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        public static BlockPoint parse(String s) {
            if (s == null || s.trim().isEmpty()) return null;
            String[] parts = s.split(",");
            if (parts.length != 4) return null;
            try {
                String world = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                return new BlockPoint(world, x, y, z);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public Location toLocation() {
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x + 0.5, y, z + 0.5);
        }

        @Override
        public String toString() {
            return world + "," + x + "," + y + "," + z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPoint)) return false;
            BlockPoint bp = (BlockPoint) o;
            return x == bp.x && y == bp.y && z == bp.z && Objects.equals(world, bp.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z);
        }
    }
}
