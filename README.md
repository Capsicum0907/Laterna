# Laterna

English | [日本語](README.ja.md)

Adds lamps in the sixteen dye colours, in several shapes, plus glass that is its own
light and one block that takes light away. Lit, they give off light level 15.

- Shapes: lamp, slab, panel, spotlight, bulb, fixture, rod and cased lamp.
- Slabs and panels are separate horizontal and vertical blocks, and one crafts into the
  other.
- The spotlight sits flush in the surface it is placed on, with no thickness at all.
- Lamps come two ways: one redstone switches on, one redstone switches off.
- Every framed shape comes three ways: the frame in the lamp's own colour, fixed black,
  or fixed white, decided by what the frame is made of — stone, polished blackstone or
  quartz.
- The colours are the game's own dye values, so a lamp sits correctly beside wool,
  concrete and terracotta of the same name.
- One dye colours eight lamps of a kind at a time.

## Target

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.248 |
| Java | 21 |

## Build

```
run.bat                   # compile and launch a dev client
gradlew build             # produce the jar
gradlew runGameTestServer # run every game test, headless, then exit
gradlew runData           # regenerate models, textures, recipes and language
```

`JAVA_HOME` must point at a JDK 21, or `java` must be on `PATH`.

## Design

See [docs/design.md](docs/design.md).

## License

MIT.
