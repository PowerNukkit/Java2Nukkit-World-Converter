# Missing Features
Unfortunately not everything can be migrated, some things are not supported by the Minecraft Bedrock Edition, Nukkit 
or by the tool itself.

Some to address this issue some items and blocks will be replaced by others and some others will be removed from the 
world.

See also [the list of replaced blocks and items](REPLACEMENTS.md).

## Unsupported by the tool
These things could be converted but are not supported by the tool yet:
* **Entities**: No entity will be converted, with exception of item frames which are blocks in Bedrock Edition
* **Potion effects**: They will be converted as water potion for now
* **Dropped items**: Will loose the `Owner` and `Thrower` tags because Java Edition accounts are not the same as Bedrock Edition accounts. 

## Unsupported by Minecraft Bedrock Edition
These things have some differences from Java Edition and needs to be treated specially:
* **Item Frames**: They are block in Bedrock Edition, not entities, so they must not overlap any block
to be converted, otherwise it will be skipped. Item Frames facing down or up aren`t supported by Bedrock Edition and will
be ignored.
* **Lever and Buttons**: Java Edition allows you to place them facing north, south, east and west when place on the bottom 
or the top part of a block. Bedrock Edition only supports south or east in that condition, so levers and buttons pointing to north
will point to south and levers and buttons pointing to west will point to east.
* **HideFlags**: The tag is not supported, it will be migrated but it will be ignored by the client
* **Debug Stick and customized states**: The block states in Bedrock Edition are not so customizable as it is in Java,
so customized blocks may have theirs properties reversed, for example if you made a disconnected fence wall, they will
be connected after the conversion
* **Spawn Eggs**: which spawns unsupported entities will be black and won't spawn anything.
* **Custom Player Head**: Player head with skins will loose the skin. The Bedrock Edition doesn't support it.
* **Blocks (+NBT)**: We are unable to pick blocks with it's NBT inside the item, so these blocks will loose the NBT tag.
* **Written Books and Signs**: With custom events will loose their events as they are unsupported by Bedrock Edition (click and hover events)
* **Big mushroom blocks**:  doesn't have all possible states in Bedrock Edition. Only states which occur when a red big 
                            mushroom is grown are supported. Other custom states will be changed to show cap on all
* **mushroom_stem**: will always be converted to the red mushroom stem as Java Edition has only one block type for both 
                    mushrooms while Bedrock Edition has stems for each of them.
* **Tipped Arrow**: of type strong_slowness (-60% Speed) isn't supported by Bedrock Edition and will be converted to normal slowness.
* **Spectral Arrow**: is missing on Bedrock Edition and will be converted to regular arrows
* **Fireworks rockets and stars**: with customized colors will always be converted to blue because it's not supported by Bedrock Edition.
* **Suspicious Stew**: Is not available in Bedrock Edition and will be converted to a normal mushroom stew
* **All potions types**: Custom effects and colors aren't supported and they will revert back to the normal potion indicated in the `Potion` NBT tag.
    Also, some potions aren't supported and they will be converted as stated in the [replacements file](REPLACEMENTS.md)

## Unsupported by Nukkit
These things won't work because it's a missing feature or a bug in Nukkit servers:
* **Pistons**: All pistons will loose their head and will not work at all.
* **Dispensers**: They will always face the same direction and will not work.
* **Water-logging**: All water-logged blocks will loose they water on conversion. 
* **Spawn Eggs**: Custom JSON values for entities will not work.
* **Book and Quill**: Writable Books will be converted and will be readable but won't be editable.
* **Attribute Modifiers**: Nukkit doesn't seems to have any support to attribute modifiers.
* **Falling Block**: Will not hurt entities on landing. Nukkit also supports only the block id and data value so
`TileEntityData`, `Time`, `DropItem`, `HurtEntities`, `FallHurtMax` and `FallHurtAmount` will be ignored. 
* **Experience orbs**: Will be migrated but will loose it's value because Nukkit does not save and neither load it, 
so collecting it will give nothing.
* **Open maps**: They are not supported by Nukkit so no map content will be migrated
