[br.com.gamemods.j2nwc](../index.md) / [WorldConverter](./index.md)

# WorldConverter

`class WorldConverter`

Prepare a conversion from a Java World 1.14+ to Nukkit.

Please check [this page](https://gamemodsbr.github.io/Java2Nukkit-World-Converter/) for details about the requirements and expectations.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `WorldConverter(from: `[`File`](https://docs.oracle.com/javase/6/docs/api/java/io/File.html)`, to: `[`File`](https://docs.oracle.com/javase/6/docs/api/java/io/File.html)`)`<br>Prepare a conversion from a Java World 1.14+ to Nukkit. |

### Properties

| Name | Summary |
|---|---|
| [from](from.md) | `val from: `[`File`](https://docs.oracle.com/javase/6/docs/api/java/io/File.html)<br>The Java world folder |
| [regionFilter](region-filter.md) | `var ~~regionFilter~~: `[`MutableSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html)`<`[`RegionPos`](../-region-pos/index.md)`>`<br>A collection which determines which region files will be converted. When empty all regions are converted. |
| [regions](regions.md) | `var regions: `[`MutableSet`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html)`<`[`RegionPosition`](../-region-position.md)`>`<br>A collection which determines which region files will be converted. When empty all regions are converted. |
| [skipSkinHeads](skip-skin-heads.md) | `var skipSkinHeads: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Determines if player heads which contains custom skins should be skipped. |
| [to](to.md) | `val to: `[`File`](https://docs.oracle.com/javase/6/docs/api/java/io/File.html)<br>The world folder that will be created for the Nukkit's world |

### Functions

| Name | Summary |
|---|---|
| [convert](convert.md) | `fun convert(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Executes the conversion in the current thread. Will take a while to complete. |
