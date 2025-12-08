package it.frafol.geyserautoupdate;

import lombok.SneakyThrows;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
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

    private ExtensionLogger logger;
    private UpdateConfig config;

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.logger = logger();
        if (!loadConfig()) {
            logger.warning("The configuration is not valid. Disabling the extension.");
            geyserApi().extensionManager().disable(this);
        }
    }

    @Subscribe
    @SneakyThrows
    public void onShutdown(GeyserShutdownEvent event) {
        if (config.extensions != null) downloadAndReplaceExtensions();
        if (config.updateCoreEnabled && config.coreDownloadUrl != null && !config.coreDownloadUrl.isBlank()) downloadAndReplaceCore();
    }

    private static class UpdateConfig {
        public boolean updateCoreEnabled;
        public String coreDownloadUrl;
        public String geyserName;
        public List<ExtensionEntry> extensions;
    }

    private static class ExtensionEntry {
        public String downloadUrl;
        public String targetFileName;
    }

    @SneakyThrows
    private boolean loadConfig() {
        Path cfgPath = dataFolder().resolve("config.yml");
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

    private void downloadAndReplaceCore() throws IOException {
        URL url = new URL(config.coreDownloadUrl);
        logger.info("Downloading Geyser from URL: " + url);
        Path installDir = dataFolder().resolveSibling("..");
        if (!config.geyserName.equalsIgnoreCase("Geyser-Standalone")) installDir = dataFolder().resolveSibling("...");
        Path target = installDir.resolve(config.geyserName + ".jar").normalize();
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        logger.info("Geyser has been downloaded successfully.");
    }
}
