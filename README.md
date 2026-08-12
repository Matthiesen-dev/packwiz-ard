# PackWiz-ard

<div>
  <img src="https://mods.matthiesen.dev/badges/matthiesenCore.svg" alt="Matthiesen Core">
</div>

Load and update your server PackWiz modpack with ease. PackWiz-ard is a server-side mod that integrates with the PackWiz 
ecosystem, allowing server owners to easily load and update their modpacks using PackWiz. With PackWiz-ard, you can keep 
your server's modpack up to date with the latest versions of mods, and easily manage your modpack's dependencies.

## Features
- **Easy configuration:** Simply provide the URL to your PackWiz `pack.toml` file in the `/config/packwiz_ard/common.toml` file.
- **Easy updating:** Use the `/packwizard update` command to download and load the mods specified in your `pack.toml` file, and keep them up to date with the latest versions.
- **Automatic updates:** Enable `auto_update` in the config and set `auto_update_interval_minutes` to run the update process on a schedule.
- **Server-side mod:** PackWiz-ard is a server-side mod, so it does not require any client-side installation. This means that players can join your server without needing to install any mods on their end.
- **Dependency management:** PackWiz-ard will automatically manage your modpack's dependencies, ensuring that all required mods are downloaded and loaded correctly.
- **Flexible Modloader support:** PackWiz-ard supports both Fabric and NeoForge modpacks, so you can use it with a wide variety of modpacks.
- **Lightweight:** PackWiz-ard is designed to be lightweight and efficient, so it won't add any unnecessary overhead to your server.
- **WebHook support:** PackWiz-ard supports sending Discord webhooks, allowing you to notify your discord server when updates are in progress or completed. Or changes to the mod's configuration are made.
- **Client update screen:** A title-screen button opens a client update screen that validates the configured `pack.toml` link, shows whether an update is available, and lets you run the client update manually.
- **Refresh control:** The client update screen can refresh automatically at a lightweight interval, or you can switch it to manual-only refresh mode.
- **Restart reminder:** After a client update completes, the screen will tell you to restart the game before the new files take effect.

## Requirements

- [Matthiesen Core](https://modrinth.com/mod/matthiesen-core)
- [Fabric API](https://modrinth.com/mod/fabric-api) (Fabric only)
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) (Fabric only)

### Optional Dependencies

- [Matthiesen Core Webhooks](https://modrinth.com/project/XP5CfD30) - Used for sending Discord webhooks

## PackWiz Integration

PackWiz-ard integrates with the PackWiz ecosystem by using the PackWiz `pack.toml` file to determine which modpack to load and update. 
To use PackWiz-ard, simply provide the web URL to your PackWiz `pack.toml` file in the mod's config, and PackWiz-ard will take care of 
the rest. Just run the `/packwizard update` command to have PackWiz-ard download and load the mods specified in your `pack.toml` file, 
and keep them up to date with the latest versions.

> To learn more about PackWiz checkout their [Documentation](https://packwiz.infra.link/), or their [Github](https://github.com/packwiz/packwiz).

## Client Updates

When the game reaches the title screen, PackWiz-ard adds a button that opens a dedicated client update screen.
That screen shows the current `pack.toml` validation result, whether a client update is available, and whether the client needs to be restarted.

By default, the screen performs lightweight refresh checks while it is open. If you prefer, you can switch refresh mode to manual-only from the screen itself.
Client updates are always started manually; PackWiz-ard does not try to auto-install updates on your behalf.

## Commands

- `/packwizard update` - Runs an immediate update.
- `/packwizard link <url>` - Sets your `pack.toml` URL.
- `/packwizard minimumPermissionLevel <0-4>` - Sets the minimum permission level for command usage.
- `/packwizard autoUpdate <true|false>` - Enables or disables scheduled automatic updates.
- `/packwizard autoUpdateInterval <minutes>` - Sets the schedule interval used by auto-update.
- `/packwizard autoUpdateStatus` - Shows whether auto-update is enabled and when the next run should happen.

## Docs

Documentation for this mod can be found at [mods.matthiesen.dev](https://mods.matthiesen.dev/packwiz-ard/)

## Version Compatibility

| Minecraft Version | Mod Version |
|-------------------|-------------|
| 1.21.1            | 1.x.x       |

## FastStats Metrics

This mod uses [FastStats](https://faststats.dev) to collect anonymous usage statistics. This helps the developer understand
how this mod is being used and improve it over time. You can learn more about the data collected and how it is used by visiting
[FastStats: Information](https://faststats.dev/info).

You can also view the data collected by this mod on the [FastStats: PackWiz-ard](https://faststats.dev/project/packwiz-ard) page.

To opt out of this data collection, set the `enabled` property to `false` in the `<game_directory>/config/matthiesen_core/metrics.properties` file.

## License

MIT - see `LICENSE`.
