# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
- [#38](https://github.com/GameModsBR/Java2Nukkit-World-Converter/issues/38) 
The entire path is shown in Usage at --help

### Added
- `WorldConveter.regions` to filter regions using the Region-Manipulator's `RegionPos`.
- Type alias `RegionPosition` to help the conversion from the deprecated `RegionPos` to Region-Manipulator's `RegionPos`
- `RegionPos.toRegionManipulator()` to convert the object to the equivalent's Region-Manipulator object.

### Changed
- Updated Region-Manipulator to `1.0.1`
- Deprecated `RegionPos`. Users should use the one provided by Region-Manipulator.
- Deprecated `WorldConverter.regionFilter`. Users should use `WorldConverter.regions` instead.

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
