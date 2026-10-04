package dev.whersss.altarLegendaryWH.altars;

import dev.whersss.altarLegendaryWH.AltarLegendaryWH;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AltarLanguageManager {

    private static final String DEFAULT_LANGUAGE = "ru_RU";
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("ru_RU", "en_US");

    private final AltarLegendaryWH plugin;
    private YamlConfiguration messages;
    private String language = DEFAULT_LANGUAGE;

    public AltarLanguageManager(AltarLegendaryWH plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        String configuredLanguage = plugin.getConfig().getString("lang", null);
        if (configuredLanguage == null) {
            configuredLanguage = plugin.getAltarsConfig().getString("language", DEFAULT_LANGUAGE);
        }
        configuredLanguage = configuredLanguage.trim();

        boolean invalidLanguage = !SUPPORTED_LANGUAGES.contains(configuredLanguage);
        language = invalidLanguage ? DEFAULT_LANGUAGE : configuredLanguage;

        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }

        for (String supportedLanguage : SUPPORTED_LANGUAGES) {
            String resourcePath = "messages/" + supportedLanguage + ".yml";
            File localeFile = new File(plugin.getDataFolder(), resourcePath);
            if (!localeFile.exists()) {
                plugin.saveResource(resourcePath, false);
            }
        }

        File localeFile = new File(plugin.getDataFolder(), "messages/" + language + ".yml");
        messages = YamlConfiguration.loadConfiguration(localeFile);

        try (InputStream stream = plugin.getResource("messages/" + language + ".yml")) {
            if (stream != null) {
                YamlConfiguration bundledMessages = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                messages.setDefaults(bundledMessages);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not load bundled language defaults: " + exception.getMessage());
        }

        if (invalidLanguage) {
            plugin.getLogger().warning(plain("language.invalid",
                    "language", configuredLanguage,
                    "fallback", DEFAULT_LANGUAGE));
        }
    }

    public Component component(String key, String... replacements) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(replace(getRaw(key), replacements));
    }

    public List<Component> components(String key, String... replacements) {
        List<Component> result = new ArrayList<>();
        List<String> lines = messages.getStringList(key);
        if (lines.isEmpty()) {
            lines = List.of("&cMissing message list: " + key);
        }
        for (String line : lines) {
            result.add(LegacyComponentSerializer.legacyAmpersand().deserialize(replace(line, replacements)));
        }
        return result;
    }

    public String plain(String key, String... replacements) {
        return PlainTextComponentSerializer.plainText().serialize(component(key, replacements));
    }

    public String getLanguage() {
        return language;
    }

    private String getRaw(String key) {
        String message = messages.getString(key);
        return message != null ? message : "&cMissing message: " + key;
    }

    private String replace(String message, String... replacements) {
        String result = message;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return result;
    }
}
