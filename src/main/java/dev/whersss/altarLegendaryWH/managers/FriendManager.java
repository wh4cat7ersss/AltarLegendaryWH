package dev.whersss.altarLegendaryWH.managers;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FriendManager {
    private final AltarLegendaryWH plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, Set<UUID>> friendsCache = new HashMap<>();

    public FriendManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "friends.yml");
        loadFriends();
    }

    private void loadFriends() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            UUID playerUUID = UUID.fromString(key);
            Set<UUID> playerFriends = new HashSet<>();
            List<String> list = config.getStringList(key);
            for (String friendStr : list) {
                playerFriends.add(UUID.fromString(friendStr));
            }
            friendsCache.put(playerUUID, playerFriends);
        }
    }

    public void saveFriends() {
        for (Map.Entry<UUID, Set<UUID>> entry : friendsCache.entrySet()) {
            List<String> list = new ArrayList<>();
            for (UUID f : entry.getValue()) list.add(f.toString());
            config.set(entry.getKey().toString(), list);
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean isFriend(UUID player, UUID target) {
        return friendsCache.containsKey(player) && friendsCache.get(player).contains(target);
    }

    public Set<UUID> getFriends(UUID player) {
        return friendsCache.getOrDefault(player, new HashSet<>());
    }

    public boolean addFriend(UUID player, UUID target) {
        Set<UUID> friends = getFriends(player);
        if (friends.size() >= 2) return false;
        friends.add(target);
        friendsCache.put(player, friends);
        saveFriends();
        return true;
    }

    public void removeFriend(UUID player, UUID target) {
        if (friendsCache.containsKey(player)) {
            friendsCache.get(player).remove(target);
            saveFriends();
        }
    }
}