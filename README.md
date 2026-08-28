# Laterna

Lamps, in the sixteen dye colours, in several shapes.

*Laterna* is Latin for a lantern.

> **Status: the cube and the recessed spotlight — 48 blocks.** Everything else in the
> table below is still to come. Nine game tests cover what they claim, except where a
> test cannot reach: see the note under the spotlight.

## Target

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.248 |
| Java | 21 |

## Design

**One product is the source of everything.** A lamp is a `Shape`, a `Wiring` and a
`DyeColor`, and nothing else. Registration, block models, textures, loot tables, recipes,
language and tags are all derived from that single product at data-generation
time. Adding a shape is one entry in an enum; adding a colour is not possible,
because the sixteen come from the game.

This is the whole reason the mod is worth writing. The reference implementation
of this idea — Simply Light — writes its 160 blocks out one constant at a time,
and repeats a `switch` over the colour names once per shape class. Nothing is
wrong with the result; it is just 160 lines that a product would have written.

**Nothing under `src/generated` is written by hand, and neither are the textures.**
A shape carries a grayscale master drawn from a formula, not a PNG, so the
repository holds no images at all. The master follows one convention: `0.5` is
the dye colour untouched, below that darkens it, above that carries it towards
white. A frame comes out at `0.34`, a glowing face at `0.80`.

A shape can have more than one layer. Parts that must *not* take the colour — the grey
ring around a spotlight — are a layer of their own, drawn once in a fixed grey and laid
over the tinted one **in the model, not in the texture**. So the ring is one file for all
sixteen colours rather than sixteen composites, and the lens can be told to draw at full
brightness while the fitting around it is not.

**No block entities and no particles.** Light comes from
`lightLevel(state -> state.getValue(LIT) ? 15 : 0)`, and redstone only chooses
which state the block is in. A lamp costs the game exactly what a stone block costs.

**Switched immediately, both ways.** Vanilla's redstone lamp waits four ticks before
going dark so a signal with a one-tick gap does not blink. This one does not: the delay
needs a scheduled tick whose condition has to be wiring-aware, and getting that backwards
is invisible until an inverted lamp behaves strangely. If the flicker turns out to matter
it goes in `LampBlock#lit`, which is the one place either wiring asks the question.

**Inverted lamps are separate blocks, and have to be.** `LIT` is a block state, so
it does not survive being carried as an item; a lamp always returns to its default
when placed. "Normal" and "inverted" are therefore two blocks with the same
appearance whose only difference is that default, converted into each other by
crafting rather than by a lever.

### Shapes

Shapes that need no behaviour of their own are in from the start. The two that do
are not — not because they are hard, but because they are a different kind of work,
and mixing the two kinds is what makes a scaffold stall.

| Shape | Blocks | Note |
|---|---|---|
| **Lamp** | **32** | **Done.** Full cube, redstone, normal + inverted |
| Slab | 16 | 8px, any of six faces, waterloggable |
| Panel | 16 | 4px, likewise |
| **Recessed spotlight** | 16 | **Done.** Zero thickness, a round lens in a grey ring |
| Bulb | 16 | Small fitting, always lit |
| Fixture | 16 | Wall/ceiling/floor fitting |
| Rod | 16 | Strip light |
| *Edge strip* | — | Deferred: shaping it to its neighbours needs a UI |
| *Lamp post* | — | Deferred: three blocks that must stay consistent |

That is **128 blocks** from eight enum entries.

### The spotlight has no thickness

Not a thin box — a plate with none. A model element whose `from` and `to` agree on
one axis is a legal flat quad, and the game itself ships three of them:
`glow_lichen` (a lit one, at that), `vine` and `lily_pad`, standing 0.1, 0.8 and
0.25 pixels off the face they cling to. This one follows `glow_lichen` at 0.1 —
close enough to read as flush, far enough not to z-fight.

Three things follow from having no thickness:

- **The collision shape is not the model.** A `VoxelShape` of zero thickness is a
  block that cannot be selected or broken. The shape is one pixel deep; only the
  drawing is flat. ⚠ The six shapes are *derived from the direction*, never listed:
  the first pass listed them, transcribed east and west the wrong way round, and put
  the outline of every east- or west-facing light a whole block from the light. Five
  of six were right, which is why it looked fine until one was pointed at.
- **Round comes from the alpha channel**, so the model declares
  `"render_type": "minecraft:cutout"`. Without it the transparent corners are black.
  ⚠ And cutout keeps or discards a pixel rather than blending it, so a softened edge
  cannot be made of alpha. The lens is softened onto the ring in *colour* instead —
  which is why the ring is a solid disc behind the lens rather than an annulus around
  it. The rim's own outline stays hard, as every round thing in the game does, at the
  one radius whose rasterisation is a clean circle rather than a cog.
- **The lens is drawn at full brightness** — `"neoforge_data": {"block_light": 15,
  "sky_light": 15}` on that element, and deliberately not on the ring, so the fitting
  reads as metal and the lens as light. (NeoForge renamed this to `light_emission`
  in 21.11; 1.21.1 predates that.)

And one that does not follow from the geometry at all: **a plate cannot be its own item
model.** In the hand and in a slot the block model is seen at an angle from which a shape
with no thickness is a line. Vanilla gives its flat blocks a flat *item* model instead —
`item/generated` over the texture layers — and so does this.

⚠ **The one thing no test reaches.** Which of the six faces the plate is drawn on comes
from a rotation table in the model generator, taken from vanilla's own `glow_lichen`
blockstate. A game test can assert the block faces up; it cannot assert the plate was
drawn on the floor rather than the ceiling. Invert the `getOpposite()` and every test
still passes with every light on the wrong surface. That one is checked by looking.

### Recipes

Each shape has exactly one recipe of its own, in white. The cube is glowstone in a frame
of stone around a pinch of redstone — or around a redstone torch, which is the inverted
one, and is the difference between the two in the world as well as on the bench. The
spotlight is the same glowstone in a ring of iron nuggets, which is the thing it is: a
lens in a metal rim, and cheap, because eight come out.

Every other colour is eight of the same thing around a dye, the way the game dyes glass,
taken through a tag per shape and wiring so that dyeing an inverted lamp gives an inverted
lamp back and no recipe ever names a colour.

### What the colours are, and are not

Vanilla light is monochrome. Sixteen colours means sixteen coloured *textures*
emitting the same white light. Coloured illumination is a shader-side feature and
is not what this mod does.

## Build

```
run.bat                   # compile and launch a dev client - double-clickable
gradlew build             # produce the jar
gradlew runGameTestServer # run every game test, headless, then exit
gradlew runData           # regenerate models, textures, recipes and language
```

`JAVA_HOME` must point at a JDK 21, or `java` must be on `PATH`.

## Roadmap

- [x] **0** — scaffold; the mod loads
- [x] **1** — the cube, sixteen colours, redstone, normal and inverted, and the
      generator that produces all of it: blocks, models, textures, loot, recipes,
      language, tags and a creative tab, none of which names a colour
- [x] **2** — the recessed spotlight: a second shape, a third wiring (always lit, and so
      no `LIT` state at all), a second texture layer, and its own geometry and item model,
      added without touching how the first shape works
- [ ] **3** — slab and panel, on any of six faces
- [ ] **4** — bulb, fixture, rod
- [ ] **5** — each shape checked by game tests as it lands, rather than by eye

## Related

One of a set of small, independent mods, each doing one thing and depending on
none of the others: [Fodina](https://github.com/Capsicum0907/Fodina),
[Trivium](https://github.com/Capsicum0907/Trivium),
[Magnes](https://github.com/Capsicum0907/Magnes),
[Cella](https://github.com/Capsicum0907/Cella),
[Acervus](https://github.com/Capsicum0907/Acervus),
[Fornax](https://github.com/Capsicum0907/Fornax),
[Accumulator](https://github.com/Capsicum0907/Accumulator).

Cella already generates its textures from code rather than storing them; this mod
takes the same approach.

## License

Not decided yet. Until it is, the metadata says All Rights Reserved.

## Sources

- Simply Light, the mod this one follows in shape:
  [CurseForge](https://www.curseforge.com/minecraft/mc-mods/simply-light) ·
  [source, GPL-3.0](https://github.com/Flanks255/simplylight) (branch `1.21.1-multi`).
  Read to check how it is put together; no code is taken from it.
- [NeoForged docs 1.21.1 — Models](https://docs.neoforged.net/docs/1.21.1/resources/client/models/)
  for `render_type` and `neoforge_data`.
- [NeoForge 21.11 release notes](https://neoforged.net/news/21.11release/) for the
  later rename to `light_emission`.
- The flat-quad measurements are from `assets/minecraft/models/block/` in the
  1.21.1 client jar.
