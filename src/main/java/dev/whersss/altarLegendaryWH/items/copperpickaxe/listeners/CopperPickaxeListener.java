package dev.whersss.altarLegendaryWH.items.copperpickaxe.listeners;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import dev.whersss.altarLegendaryWH.items.copperpickaxe.CopperPickaxeItem;
import dev.whersss.altarLegendaryWH.utils.TextUtils;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CopperPickaxeListener implements Listener {

    private final AltarLegendaryWH plugin;
    private final Map<UUID, BlockFace> lastFace = new HashMap<>();

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

        if (mainIsPickaxe) {
            player.getInventory().setItemInMainHand(pickaxe);
        } else {
            player.getInventory().setItemInOffHand(pickaxe);
        }

        boolean enabled = CopperPickaxeItem.isModeEnabled(pickaxe);
        String status = enabled
                ? "&a" + plugin.tr("Включено", "Enabled")
                : "&c" + plugin.tr("Выключено", "Disabled");
        player.sendActionBar(TextUtils.legacy("&63x3 " + plugin.tr("Копание: ", "Mining: ") + status));
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 2.0f);
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
        lastFace.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!CopperPickaxeItem.isCopperPickaxe(tool) || !CopperPickaxeItem.isModeEnabled(tool)) {
            return;
        }

        BlockFace face = lastFace.getOrDefault(player.getUniqueId(), BlockFace.SELF);
        List<Block> area = computeArea(event.getBlock(), face);
        Set<Material> blocked = getBlockedMaterials();

        for (Block block : area) {
            if (block.getType().isAir() || blocked.contains(block.getType())) {
                continue;
            }
            player.sendBlockDamage(block.getLocation(), 0.5f, block.hashCode());
        }
    }
    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Player player = event.getPlayer();
        BlockFace face = lastFace.getOrDefault(player.getUniqueId(), BlockFace.SELF);
        List<Block> area = computeArea(event.getBlock(), face);

        for (Block block : area) {
            player.sendBlockDamage(block.getLocation(), 0.0f, block.hashCode());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!CopperPickaxeItem.isCopperPickaxe(tool) || !CopperPickaxeItem.isModeEnabled(tool)) {
            return;
        }

        BlockFace face = lastFace.getOrDefault(player.getUniqueId(), BlockFace.SELF);
        List<Block> area = computeArea(event.getBlock(), face);
        World world = event.getBlock().getWorld();
        Set<Material> blocked = getBlockedMaterials();

        for (Block block : area) {
            player.sendBlockDamage(block.getLocation(), 0.0f, block.hashCode());

            if (block.getType().isAir() || blocked.contains(block.getType())) {
                continue;
            }

            SoundGroup soundGroup = block.getBlockSoundGroup();
            BlockData blockData = block.getBlockData();
            Location center = block.getLocation().add(0.5, 0.5, 0.5);

            boolean broken = block.breakNaturally(tool);
            if (broken) {
                world.playSound(center, soundGroup.getBreakSound(), soundGroup.getVolume(), soundGroup.getPitch());
                world.spawnParticle(Particle.BLOCK, center, 40, 0.3, 0.3, 0.3, 0, blockData);
            }
        }
    }

    private Set<Material> getBlockedMaterials() {
        Set<Material> materials = new HashSet<>();
        for (String entry : plugin.getConfig().getStringList("copper-pickaxe.not-breakable-3x3")) {
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
}