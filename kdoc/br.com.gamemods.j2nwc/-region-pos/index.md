[br.com.gamemods.j2nwc](../index.md) / [RegionPos](./index.md)

# RegionPos

`data class ~~RegionPos~~`
**Deprecated:** Already provided by Region-Manipulator

A region position extracted from the region file name.

`r.-2.3.mca` must be `Region(-2,3)` for example

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `RegionPos(mcaFileName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`)`<br>Parses a region file name. Only support valid names like `r.-3.2.mca`.`RegionPos(xPos: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, zPos: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`<br>A region position extracted from the region file name. |

### Properties

| Name | Summary |
|---|---|
| [xPos](x-pos.md) | `val xPos: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The first number in the region file name. May be negative. |
| [zPos](z-pos.md) | `val zPos: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The second number in the region file name. May be negative. |

### Functions

| Name | Summary |
|---|---|
| [toRegionManipulator](to-region-manipulator.md) | `fun toRegionManipulator(): `[`RegionPosition`](../-region-position.md)<br>Converts this deprecated object to it's replacement. |
