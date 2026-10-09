package dev.whersss.altarLegendaryWH.items.copperpickaxe.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.items.copperpickaxe.CopperPickaxeItem;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundGroup;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class CopperPickaxeListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, BlockFace> lastFace = new HashMap<>();
    private final Map<UUID, List<Block>> previewBlocks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> previewTasks = new HashMap<>();

    public CopperPickaxeListener(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean mainIsPickaxe = CopperPickaxeItem.isCopperPickaxe(mainHand);
        boolean offIsPickaxe = CopperPickaxeItem.isCopperPickaxe(offHand);

        if (!mainIsPickaxe && !offIsPickaxe) {
            return;
        }

        event.setCancelled(true);

        ItemStack pickaxe = mainIsPickaxe ? mainHand : offHand;
        CopperPickaxeItem.toggleMode(pickaxe);

        boolean enabled = CopperPickaxeItem.isModeEnabled(pickaxe);
        String status = enabled
                ? "&a" + plugin.tr("Включено", "Enabled")
                : "&c" + plugin.tr("Выключено", "Disabled");
        player.sendActionBar(TextUtils.legacy("&63x3 " + plugin.tr("Копание: ", "Mining: ") + status));
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 2.0f);
        plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        lastFace.put(event.getPlayer().getUniqueId(), event.getBlockFace());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPreview(event.getPlayer());
        lastFace.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!CopperPickaxeItem.isCopperPickaxe(tool) || !CopperPickaxeItem.isModeEnabled(tool)) {
            return;
        }

        Block origin = event.getBlock();
        if (origin.getType() == Material.BEDROCK || origin.getType().getHardness() < 0.0f) {
            return;
        }

        BlockFace face = lastFace.getOrDefault(player.getUniqueId(), BlockFace.SELF);
        List<Block> area = computeArea(origin, face);
        Set<Material> blocked = getBlockedMaterials();

        clearPreview(player);
        previewBlocks.put(player.getUniqueId(), area);
        startPreviewTask(player, blocked);
    }
    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        clearPreview(event.getPlayer());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!CopperPickaxeItem.isCopperPickaxe(tool) || !CopperPickaxeItem.isModeEnabled(tool)) {
            return;
        }

        Block origin = event.getBlock();
        if (origin.getType() == Material.BEDROCK || origin.getType().getHardness() < 0.0f) {
            event.setCancelled(true);
            return;
        }

        BlockFace face = lastFace.getOrDefault(player.getUniqueId(), BlockFace.SELF);
        List<Block> area = computeArea(origin, face);
        World world = origin.getWorld();
        Set<Material> blocked = getBlockedMaterials();

        clearPreview(player);
        for (Block block : area) {
            if (block.equals(origin)) {
                continue;
            }
            if (block.getType().isAir() || block.getType() == Material.BEDROCK || block.getType().getHardness() < 0.0f || blocked.contains(block.getType())) {
                continue;
            }

            BlockBreakEvent subEvent = new BlockBreakEvent(block, player);
            Bukkit.getPluginManager().callEvent(subEvent);
            if (subEvent.isCancelled()) {
                continue;
            }

            SoundGroup soundGroup = block.getBlockSoundGroup();
            BlockData blockData = block.getBlockData();
            Location center = block.getLocation().add(0.5, 0.5, 0.5);

            boolean broken = block.breakNaturally(tool);
            if (broken) {
                world.playSound(center, soundGroup.getBreakSound(), soundGroup.getVolume(), 1.1f);
                world.spawnParticle(Particle.BLOCK, center, 40, 0.3, 0.3, 0.3, 0, blockData);
            }
        }
    }

    private Set<Material> getBlockedMaterials() {
        Set<Material> materials = new HashSet<>();
        materials.add(Material.BEDROCK);
        materials.add(Material.BARRIER);
        for (String entry : plugin.getItemsConfig().getStringList("copper-pickaxe.not-breakable-3x3")) {
            Material material = Material.matchMaterial(entry.toUpperCase(Locale.ROOT));
            if (material != null) {
                materials.add(material);
            }
        }
        return materials;
    }

    private List<Block> computeArea(Block origin, BlockFace face) {
        List<Block> blocks = new ArrayList<>();
        if (face == null || face == BlockFace.SELF) {
            return blocks;
        }

        BlockFace digDirection = face.getOppositeFace();
        int[] perp1;
        int[] perp2;

        if (face.getModY() != 0) {
            perp1 = new int[]{1, 0, 0};
            perp2 = new int[]{0, 0, 1};
        } else if (face.getModX() != 0) {
            perp1 = new int[]{0, 1, 0};
            perp2 = new int[]{0, 0, 1};
        } else {
            perp1 = new int[]{1, 0, 0};
            perp2 = new int[]{0, 1, 0};
        }

        int dx = digDirection.getModX();
        int dy = digDirection.getModY();
        int dz = digDirection.getModZ();

        for (int depth = 0; depth <= 1; depth++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (depth == 0 && a == 0 && b == 0) {
                        continue;
                    }
                    int offX = perp1[0] * a + perp2[0] * b + dx * depth;
                    int offY = perp1[1] * a + perp2[1] * b + dy * depth;
                    int offZ = perp1[2] * a + perp2[2] * b + dz * depth;
                    blocks.add(origin.getRelative(offX, offY, offZ));
                }
            }
        }
        return blocks;
    }

    private void clearPreview(Player player) {
        cancelPreviewTask(player);
        List<Block> blocks = previewBlocks.remove(player.getUniqueId());
        if (blocks == null) {
            return;
        }

        for (Block block : blocks) {
            player.sendBlockDamage(block.getLocation(), 0.0f, blockSourceId(block));
        }
    }

    private void startPreviewTask(Player player, Set<Material> blocked) {
        cancelPreviewTask(player);
        UUID uuid = player.getUniqueId();
        BukkitRunnable task = new BukkitRunnable() {
            private final float[] stages = new float[]{0.15f, 0.30f, 0.50f, 0.70f, 0.90f};
            private int step;

            @Override
            public void run() {
                List<Block> blocks = previewBlocks.get(uuid);
                if (blocks == null || blocks.isEmpty() || !player.isOnline()) {
                    cancelPreviewTask(player);
                    cancel();
                    return;
                }

                ItemStack tool = player.getInventory().getItemInMainHand();
                if (!CopperPickaxeItem.isCopperPickaxe(tool) || !CopperPickaxeItem.isModeEnabled(tool)) {
                    clearPreview(player);
                    cancel();
                    return;
                }

                float progress = stages[Math.min(step, stages.length - 1)];
                for (Block block : blocks) {
                    if (block.getType().isAir() || blocked.contains(block.getType())) {
                        continue;
                    }
                    player.sendBlockDamage(block.getLocation(), progress, blockSourceId(block));
                }
                step++;
            }
        };
        previewTasks.put(uuid, task);
        task.runTaskTimer(plugin, 0L, 2L);
    }

    private void cancelPreviewTask(Player player) {
        BukkitRunnable task = previewTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private int blockSourceId(Block block) {
        return Objects.hash(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }
}
