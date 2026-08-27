# Laterna

Lamps, in the sixteen dye colours, in several shapes.

*Laterna* is Latin for a lantern.

> **Status: scaffold only.** The mod loads and does nothing.

## Target

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.248 |
| Java | 21 |

## Design

**One product is the source of everything.** A lamp is a `Shape` and a `DyeColor`,
and nothing else. Registration, block models, textures, loot tables, recipes,
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
white. A frame comes out at `0.34`, a glowing face at `0.80`. Parts that must
*not* take the colour — the grey ring around a spotlight — are a separate,
untinted layer composited over the tinted one.

**No block entities and no particles.** Light comes from
`lightLevel(state -> state.getValue(ON) ? 15 : 0)`, and redstone only chooses
which state the block is in. A lamp costs the game exactly what a stone block costs.

**Inverted lamps are separate blocks, and have to be.** `ON` is a block state, so
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
| Illuminant block | 32 | Full cube, redstone, normal + inverted |
| Slab | 16 | 8px, any of six faces, waterloggable |
| Panel | 16 | 4px, likewise |
| **Recessed spotlight** | 16 | **Zero thickness.** A round lens in a grey ring |
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
  drawing is flat.
- **Round comes from the alpha channel**, so the model declares
  `"render_type": "minecraft:cutout"`. Without it the transparent corners are black.
- **The lens is drawn at full brightness** — `"neoforge_data": {"block_light": 15,
  "sky_light": 15}` on that face, and deliberately not on the ring, so the fitting
  reads as metal and the lens as light. (NeoForge renamed this to `light_emission`
  in 21.11; 1.21.1 predates that.)

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
- [ ] **1** — illuminant block, sixteen colours, redstone, normal and inverted;
      the generator that produces all of it
- [ ] **2** — the recessed spotlight, proving a shape can be added to that generator
- [ ] **3** — slab and panel, on any of six faces
- [ ] **4** — bulb, fixture, rod
- [ ] **5** — checked by game tests rather than by eye

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
