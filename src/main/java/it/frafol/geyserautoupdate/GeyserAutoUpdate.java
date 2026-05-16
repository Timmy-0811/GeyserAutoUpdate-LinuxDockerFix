package it.frafol.geyserautoupdate;

import lombok.SneakyThrows;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.util.PlatformType;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class GeyserAutoUpdate implements Extension {

    private Path cachePath;
    private ExtensionLogger logger;
    private UpdateConfig config;

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        this.logger = logger();
        if (!loadConfig()) {
            logger.warning("The configuration is not valid. Disabling the extension.");
            geyserApi().extensionManager().disable(this);
            return;
        }

        if (config.boot && geyserApi().platformType().equals(PlatformType.STANDALONE)) {
            if (isUpdateRequired()) {
                logger.info("Executing update on boot...");
                if (updateAll()) {
                    createCacheFile();
                    logger.info("Update completed. Restarting Geyser-Standalone...");
                    System.exit(0);
                } else {
                    logger.error("Update failed during boot process.");
                }
            } else {
                logger.info("Update was already performed on last boot. Skipping update check.");
                deleteCacheFile();
            }
        }
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        if (!config.boot) {
            if (config.extensions != null) downloadAndReplaceExtensions();
            if (config.updateCoreEnabled && config.coreDownloadUrl != null && !config.coreDownloadUrl.isBlank()) downloadAndReplaceCore();
        }
    }

    private boolean updateAll() {
        boolean success = true;
        if (config.extensions != null) {
            try {
                downloadAndReplaceExtensions();
            } catch (Exception e) {
                logger.error("Error updating extensions during boot.", e);
                success = false;
            }
        }
        if (config.updateCoreEnabled && config.coreDownloadUrl != null && !config.coreDownloadUrl.isBlank()) {
            downloadAndReplaceCore();
        }
        return success;
    }

    @SneakyThrows
    private void createCacheFile() {
        YamlFile yaml = new YamlFile(cachePath.toFile());
        yaml.set("updated", true);
        yaml.save();
    }

    private boolean isUpdateRequired() {
        return !cachePath.toFile().exists();
    }

    private void deleteCacheFile() {
        try {
            if (Files.deleteIfExists(cachePath)) {
                logger.info("Successfully deleted update cache file.");
            } else {
                logger.warning("Cache file not found or already deleted: " + cachePath);
            }
        } catch (IOException exception) {
            logger.error("FATAL: Unable to remove cache file " + cachePath + ".", exception);
        }
    }

    private static class UpdateConfig {
        public boolean updateCoreEnabled;
        public String coreDownloadUrl;
        public String geyserName;
        public boolean boot;
        public List<ExtensionEntry> extensions;
    }

    private static class ExtensionEntry {
        public String downloadUrl;
        public String targetFileName;
    }

    @SneakyThrows
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean loadConfig() {
        Path cfgPath = dataFolder().resolve("config.yml");
        cachePath = dataFolder().resolve("cache.yml");
        if (!cfgPath.toFile().exists()) {
            dataFolder().toFile().mkdirs();
            try (InputStream is = getClass().getResourceAsStream("/config.yml")) {
                if (is != null) {
                    Files.copy(is, cfgPath);
                } else {
                    cfgPath.toFile().createNewFile();
                }
            }
        }
        YamlFile yaml = YamlFile.loadConfiguration(cfgPath.toFile());
        yaml.load();
        UpdateConfig cfg = new UpdateConfig();
        cfg.updateCoreEnabled = yaml.getBoolean("update-core.enabled", false);
        cfg.coreDownloadUrl = yaml.getString("update-core.download-url", "");
        cfg.geyserName = yaml.getString("update-core.geyser-type", "Geyser-Standalone");
        cfg.boot = yaml.getBoolean("boot", false);
        List<Map<?, ?>> exts = yaml.getMapList("extensions");
        if (exts != null && !exts.isEmpty()) {
            cfg.extensions = exts.stream().map(m -> {
                ExtensionEntry e = new ExtensionEntry();
                e.downloadUrl = (String) m.get("download-url");
                e.targetFileName = (String) m.get("target-file");
                return e;
            }).toList();
        }
        this.config = cfg;
        return true;
    }

    private void downloadAndReplaceExtensions() {
        for (ExtensionEntry ext : config.extensions) {
            try {
                URL url = new URL(ext.downloadUrl);
                logger.info("Downloading the extension from URL: " + url);
                Path target = dataFolder().resolveSibling(".").resolve(ext.targetFileName);
                try (InputStream in = url.openStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                logger.info("Updated extension: " + ext.targetFileName);
            } catch (IOException e) {
                logger.error("Error downloading/uploading extension: " + ext.targetFileName, e);
            }
        }
    }

    @SneakyThrows
    private void downloadAndReplaceCore() {
        URL url = new URL(config.coreDownloadUrl);

        logger.info("Downloading Geyser from URL: " + url);

        Path installDir = dataFolder().getParent();

        if (!geyserApi().platformType().equals(PlatformType.STANDALONE)) {
            installDir = installDir.getParent().getParent();
        }

        Path target = installDir
                .resolve(config.geyserName + ".jar")
                .normalize();

        logger.info("Target path: " + target);

        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("Geyser has been downloaded successfully.");
    }
}
