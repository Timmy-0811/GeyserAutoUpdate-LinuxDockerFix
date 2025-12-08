---

## 🌊 GeyserAutoUpdate

### 📋 Description

**GeyserAutoUpdate** is a powerful Geyser extension that automatically keeps your Geyser proxy and its extensions up to date. Say goodbye to manual updates — this plugin handles everything for you **when the server shuts down**!

### ✨ Features

* 🔄 **Automatic updates** for Geyser to the latest version
* ⚙️ **Auto-update extensions** configured for Geyser
* 🛠️ Easy configuration via `config.yml` to enable or disable auto-updates
* 🚪 Updates run safely during server shutdown to avoid interruptions or conflicts

### ⚙️ Configuration


```
update-core:
  enabled: true

  # Values: Geyser-BungeeCord, Geyser-Fabric, Geyser-Spigot, Geyser-Standalone, Geyser-Velocity, Geyser-ViaProxy
  geyser-type: "Geyser-Standalone"

  # Change the URL with the type of your Geyser instance, this URL is only for standalone version.
  download-url: "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/standalone"

extensions:
  - download-url: "https://github.com/frafol/GeyserAutoUpdate/releases/latest/download/GeyserAutoUpdate.jar"
    target-file: "GeyserAutoUpdate.jar"
#  - download-url: "https://github.com/MCXboxBroadcast/Broadcaster/releases/latest/download/MCXboxBroadcastExtension.jar"
#    target-file: "MCXboxBroadcastExtension.jar"

```


---
