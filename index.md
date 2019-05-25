# Minecraft Java to Nukkit World Converter
This is a command line tool that will convert Minecraft 1.14.1 anvil worlds to Nukkit's anvil world format.

Please check the [missing features file](MISSING_FEATURES.md) to be aware of everything that is not supported by 
Bedrock Edition or Nukkit.

Also take a look at the [replacements file](REPLACEMENTS.md) to be aware of all block and items replacements that are
done due to the lack of support by Nukkit or Bedrock Edition.

You may also want to see the [changelog](CHANGELOG.md) file to be aware of all changes in the tool that may impact you.

## Requeriments
Before you convert you need to optimize your world using Minecraft 1.14.1.

This tool only supports Minecraft Java Edition 1.14.1, other versions and unoptimized worlds in different versions 
may cause incorrect conversions or crashes during the conversion.

You will also need Java 1.8+ to executed the tool. It will work in all platforms supported by Java, 
this means Windows, Linux, Mac OS, and more are supported.

### How do I know if a world is optimized?
When you open a world that was created in an older version of Minecraft, the game will slowly update the chunks
to the version that you are running the game as you walk. It will only update the chunks that you loaded while walking.

If you created the world in the same Minecraft version that you are playing, than the world is already optimized to that 
version because it won't have old chunks.

If you are in doubt, optimize it.

### How do I optimize worlds
The game offers an option to eagerly update all chunks in the world at once. 

To use that follow these steps: 

1. Click the `Singleplayer` button in the main menu
2. Select the world without opening it
3. Click the `Edit` button
4. Click the `Optimize World` button
5. Click the `Backup and load` button
6. Wait until it finishes
7. Congratulations! Your world is now optimized.

## Liability and Warranty
This tool is distributed under [MIT License](LICENSE), this means that we give no warranty and take no responsibility to
any damage that this tool may cause. Please read [the license terms](LICENSE) for more details.

Most of the mappings were also made manually and humans are subject to errors. If you find any block or item being 
converted to other item or block and which that conversion isn't reported [in the replacements file](REPLACEMENTS.md), 
please, report it in the issues section, or, attempt to fix the mapping in the properties files and create a pull request.

## How to run?
Simply open your favorite terminal/console application, cmd.exe for example, navigate to the folder that you have 
downloaded the tool and execute this command:
```batch
java -jar TheToolJarFile.jar "C:\Path\To\The\Java\World\Dir" "C:\Were\The\Converted\Folder\Will\Be"
```

Don't forget to replace the directory paths and the tool jar file name.

## How do I build the project?
Just open your terminal software, cmd.exe for example, navigate to inside the project directory and type:

If you are on Linux/Unix:
```sh
chmod a+x ./gradlew
./gradlew build
```

If you are on Windows:
```sh
gradlew.bat build
```

The JAR file will be inside the `build/libs` folder. Use the one which ends with `-all`

## I have a question or I want to talk about the tool
Open an issue, it will be flagged as question or dialog and I it will be replied soon.

## Can I use it as library?
Yes you can!

Currently this tool is shared to jcenter, so make sure you have it in your repository list.

The API is very simple now and doesn't allows customizations. It can be used both on Kotlin and Java.

[Click here to see the javadoc](https://gamemodsbr.github.io/Java2Nukkit-World-Converter/javadoc/).

### Examples

#### Kotlin
```kotlin
val converter = WorldConverter(
    from = File("""Z:\Users\Fake\.minecraft\saves\Fake World"""),
    to   = File("""Z:\Fake\Nukkit\Server\worlds\FakeWorld""")
)

// Allows you to specify which region will be converter
// useful for debugging, if not changed will convert everything
converter.regionFilter = mutableListOf(RegionPos(0,0), RegionPos(-1,0))

// Executes the conversion
converter.convert()
```
#### Java
```java
WorldConverter converter = new WorldConverter(
    new File("Z:\\Users\\Fake\\.minecraft\\saves\\Fake World"),
    new File("Z:\\Fake\\Nukkit\\Server\\worlds\\FakeWorld")
);

// Allows you to specify which region will be converter
// useful for debugging, if not changed will convert everything
converter.regionFilter = Arrays.asList(new RegionPos(0,0), new RegionPos(-1,0));

// Executes the conversion
converter.convert();
```


#### Calling as CLI
You can also call it as CLI:
```kotlin
WorldConverterCLI.main(
    """Z:\Users\Fake\.minecraft\saves\Fake World""",
    """Z:\Fake\Nukkit\Server\worlds\FakeWorld"""
)
```

### Adding as dependency
#### Gradle
```groovy
repositories {
    jcenter()
}

dependencies {
    compile 'br.com.gamemods:java2nukkit-world-converter:1.0.0'    
}
```

### Maven
```xml
<repositories>
    <repository>
        <id>jcenter</id>
        <url>https://jcenter.bintray.com/</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
      <groupId>br.com.gamemods</groupId>
      <artifactId>java2nukkit-world-converter</artifactId>
      <version>1.0.0</version>
    </dependency>
</dependencies>
```

### Ivy
```xml
<resolvers>
    <bintray />
</resolvers>
<dependencies>
    <dependency org='br.com.gamemods' name='java2nukkit-world-converter' rev='1.0.0'>
      <artifact name='java2nukkit-world-converter' ext='jar' />
    </dependency>
</dependencies>
```
