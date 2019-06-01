# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- `WorldConveter.regions` to filter regions using the Region-Manipulator's `RegionPos`.
- Type alias `RegionPosition` to help the conversion from the deprecated `RegionPos` to Region-Manipulator's `RegionPos`
- `RegionPos.toRegionManipulator()` to convert the object to the equivalent's Region-Manipulator object.
- `--keep-custom-heads` argument to keep converting the player heads with custom skins as regular player heads.
- `WorldConverter.skipSkinHeads` if player heads with custom skins as regular player heads should be skipped.

### Changed
- Updated Region-Manipulator to `1.0.1`
- Deprecated `RegionPos`. Users should use the one provided by Region-Manipulator.
- Deprecated `WorldConverter.regionFilter`. Users should use `WorldConverter.regions` instead.
- Unmapped block states will now log a warning
- Colored signs will be colored using text color instead of dye color. Some colors will be a little different and all
them will be very bright. 
- [#54] Player heads with custom skins will now be skipped by default. This can be changed using `--keep-custom-heads` or `WorldConverter.skipSkinHeads` 
- [#59] `smooth_red_sandstone_slab` is now replaced with `red_sandstone_slab` instead of `acacia_slab`.  
- [#60] `red_nether_brick_stairs` is now replaced with `nether_brick_stairs` instead of `brick_stairs`.  
- [#63] `dark_prismarine_stairs` is now replaced with `stone_brick_stairs` instead of `cobblestone_stairs`.  

### Fixed
- [#38] The entire path is shown in Usage at --help
- [#39] HeightMap is not converted properly
- [#40] Biomes are not converted properly
- Exceptions when converting optimized 1.8.8 to Nukkit. ([#41], [#42], [#43], [#44], [#45], [#46], [#47], [#48])
- [#49] Nukkit crash due to an illegal conversion of generatorOption settings
- [#50] waterloggable block states migrated from optimized 1.8.8 world becomes stone.
- [#51] noteblocks migrated from optimized 1.8.8 world becomes stone.
- [#52] generatorOptions conversion for flat worlds
- [#53] The trapdoor placement and open/close state changes after conversion.
- [#55] Signs are empty after the conversion.
- [#56] The buttons placement and pressed state changes after conversion.
- [#57] Stained glasses are becoming invisible bedrock.
- [#58] `prismarine_brick_slab` and `dark_prismarine_slab` are swapped.
- [#61] [#62] `nether_brick_wall` and `end_stone_brick_wall` are swapped.
- [#64] Empty chunks being recreated.

## [1.0.0] - 2019-05-25
### Added
- Support for Minecraft 1.14.1 to Nukkit [ccd5d78](https://github.com/NukkitX/Nukkit/tree/ccd5d78aee06d6097327dc825e32d10482c79043)
- Conversion for block states to block/data
- Conversion for items in inventories
- Conversion for paintings
- Conversion for item frames
- Conversion for dropped items
- Conversion for experience orbs
- Conversion for falling blocks
- Conversion for primed TNT
- Small API for usage as library
- Option specify the region files that will be converted

[Unreleased]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/compare/a8f41900b32740648752ff214581eb8da0f928f6..v1.0.0

[#38]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/38
[#39]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/39
[#40]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/40
[#41]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/41
[#42]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/42
[#43]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/43
[#44]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/44
[#45]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/45
[#46]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/46
[#47]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/47
[#48]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/48
[#49]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/49
[#50]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/50
[#51]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/51
[#52]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/52
[#53]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/53
[#54]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/54
[#55]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/55
[#56]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/56
[#57]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/57
[#58]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/58
[#59]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/59
[#60]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/60
[#61]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/61
[#62]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/62
[#63]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/63
[#64]: https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/64
