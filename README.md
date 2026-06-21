# PackWiz-ard

<div>
  <img src="https://mods.matthiesen.dev/badges/matthiesenLibAPI.svg" alt="Matthiesen Lib API">
</div>

Load and update your server PackWiz modpack with ease. PackWiz-ard is a server-side mod that integrates with the PackWiz 
ecosystem, allowing server owners to easily load and update their modpacks using PackWiz. With PackWiz-ard, you can keep 
your server's modpack up to date with the latest versions of mods, and easily manage your modpack's dependencies.

## Requirements
- [Matthiesen Lib API](https://modrinth.com/mod/matthiesen-lib-api)

## PackWiz Integration

PackWiz-ard integrates with the PackWiz ecosystem by using the PackWiz `pack.toml` file to determine which modpack to load and update. 
To use PackWiz-ard, simply provide the web URL to your PackWiz `pack.toml` file in the mod's config, and PackWiz-ard will take care of 
the rest. Just run the `/packwizard update` command to have PackWiz-ard download and load the mods specified in your `pack.toml` file, 
and keep them up to date with the latest versions.

> To learn more about PackWiz checkout their [Documentation](https://packwiz.infra.link/), or their [Github](https://github.com/packwiz/packwiz).
 
## Features
- **Easy configuration:** Simply provide the URL to your PackWiz `pack.toml` file in the `/config/packwiz_ard/config.json` file.
- **Easy updating:** Use the `/packwizard update` command to download and load the mods specified in your `pack.toml` file, and keep them up to date with the latest versions.
- **Server-side mod:** PackWiz-ard is a server-side mod, so it does not require any client-side installation. This means that players can join your server without needing to install any mods on their end.
- **Dependency management:** PackWiz-ard will automatically manage your modpack's dependencies, ensuring that all required mods are downloaded and loaded correctly.
- **Modloader support:** PackWiz-ard supports both Fabric and NeoForge modpacks, so you can use it with a wide variety of modpacks.
- **Lightweight:** PackWiz-ard is designed to be lightweight and efficient, so it won't add any unnecessary overhead to your server.

### Planned/WIP Features
- **Automatic updates:** PackWiz-ard will automatically check for updates to your modpack and download them in the background, so you don't have to worry about manually running the update command.
- **WebHook support:** PackWiz-ard will support sending Discord webhooks, allowing you to trigger updates automatically when changes are made to your `pack.toml` file.

## Docs

Documentation for this mod can be found at [mods.matthiesen.dev](https://mods.matthiesen.dev/packwiz-ard/)

## Version Compatibility

| Minecraft Version | Mod Version |
|-------------------|-------------|
| 1.21.1            | 1.x.x       |

## License

MIT - see `LICENSE`.
