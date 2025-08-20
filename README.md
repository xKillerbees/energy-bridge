# Energy Bridge

Minecraft **NeoForge 1.21.1** mod that bridges Forge Energy (FE) between adjacent blocks.
Pulls from any neighbor that can **extract**, and pushes to any neighbor that can **receive**. 
Includes an internal buffer.

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/19372d7a-51c9-4eea-9f1a-ab7e1e9fa48d" />

The converter must be touching the side of the energy bridge directly.
Orange/Red side must be initially touching the energy bridge for it to start working. You can toggle it with the wrench on the convertor and it'll stop working (the animation on the energy bridge shows this) and back again. But if it starts on the wrong side of the convertor it will not see the update if you fix it.
Energy is output on the top and bottom.
Can connect Energy Accepter and then any block or ME Controller directly touching top or bottom of energy bridge.

## Requirements
- Java **21**
- Gradle Wrapper (included) or local Gradle
- Minecraft **1.21.1**, **NeoForge**

## Build
```bash
./gradlew clean build
```

Artifacts are in `build/libs/`.

## Versioning
- Project version is usually set in `gradle.properties` as `mod_version`, or directly in `build.gradle`.
- Keep `src/main/resources/META-INF/neoforge.mods.toml` `[[mods]].version` in sync.
- Tag releases: `git tag v1.0.1 && git push origin v1.0.1`

## Release workflow
This repository includes a GitHub Actions workflow (`.github/workflows/release.yml`):
- On tag `v*.*.*`, CI builds the mod and creates a GitHub Release with the JAR attached.
- You can also run it manually via the "Run workflow" button.

## Local testing tips
- Use a dev environment run config or copy the built JAR to your `mods/` directory for a 1.21.1 NeoForge instance.
- Break the Energy Bridge in survival — it should always drop itself.
- Place the block next to an FE source/sink to observe pulling/pushing behavior.
- Tweak capacity/IO in `EnergyBridgeBE` and rebuild.

## License
MIT — see `LICENSE`.
