# Laterna

Lamps, in the sixteen dye colours, in several shapes.

*Laterna* is Latin for a lantern.

> **Status: every form there is going to be — 176 blocks.** Nineteen game tests
> cover what they claim, except where a test cannot reach: see the note under the
> spotlight.

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
| **Lamp slab** | **16** | **Done.** 8px, lying down, placed and stacked as vanilla does |
| **Vertical lamp slab** | **16** | **Done.** The same, standing against a wall, and stacking |
| **Lamp panel** | **16** | **Done.** 4px, lying down |
| **Vertical lamp panel** | **16** | **Done.** 4px, standing |
| **Recessed spotlight** | 16 | **Done.** Zero thickness, a round lens in a grey ring |
| **Bulb** | **16** | **Done.** A base and a narrow bulb standing out of it |
| **Fixture** | **16** | **Done.** A bar on a wall, a disc on a floor |
| **Rod** | **16** | **Done.** A thin bar running the length of its cell |
| **Cased lamp** | **16** | **Done.** An opaque core inside a clear case, filling its cell |
| ~~*Edge strip*~~ | — | **Not planned.** Asked for and turned down on 2026-08-29 |
| ~~*Lamp post*~~ | — | **Not planned.** Same |

That is **128 blocks** from eight enum entries.

**What is mounted on a face falls with that face.** A recessed light is a hole in a wall
and a bulb is bolted to one; with the wall gone there is nothing holding either, so they
drop as items. A slab or a panel is a thing in its own right and stays put. ⚠ The reason
is practical rather than physical — taking a wall down should take its lights with it,
rather than leaving a field of them to be knocked out one at a time.

**A form that does not reach the edges of its face carries an inset**, and the one number
trims the box the model is built from and the outline you point at together — so a small
lamp cannot end up with a full-face hit box. ⚠ The spotlight was exactly that until it was
looked at: drawn eight pixels across, outlined at the full sixteen, and catching the
pointer anywhere near it.

**A bulb is two parts: a base against the surface and a stem standing out of it.** One box
with a lit face on it is a tile stuck to a wall; a plate flush with the surface with
something standing out of it is a lamp bolted to one. At four by four there is room for
both, because the base and the stem are different sizes rather than one inset in the other.

⚠ **A fitting is housed differently on a wall than on a ceiling, and finding that took
six passes.** A rim inside the face left too little lamp — eight by four minus a pixel each
way is six by two, a third of it. A plate around the face kept the light but made the
outline ten by six. Dropping the housing matched the outline and lost the fitting. A band
behind the light was invisible from in front. Darkening all five other faces borrowed what
a slab does, where the sides are cut material — but on a fitting the sides are the lamp and
are meant to be seen. What it is:

- **on a ceiling or a floor**, a dark plate against the surface and the lamp below it, lit
  on every side but the one it hangs from;
- **on a wall**, a hood: housing down the back and along the top — an L seen from the side —
  with the lamp filling the rest, so the light shows on the front, the underside and the
  two ends.

**A cased lamp is the same idea grown to a whole block**: a core four pixels in on every
side, inside a clear cover that fills the cell. It came out of looking at the bulb — the
bulb with its base taken off — and it is the one cube here that is always lit, so it keeps
no `LIT` state and is a plain block rather than a `LampBlock`.

⚠ **Its core wears the cube's own face, not a flat colour**, drawn with a two-pixel frame
so that shrunk onto eight pixels the frame is still a pixel. And its case is thinner than a
bulb's shell: at a bulb's size a milky film reads as glass, but stretched over a whole block
the same film is fog and the lamp inside disappears.

**A bulb is a body inside a shell.** A dark base, a dark neck, a coloured body standing out
of that, and a translucent shell a shade larger over the body. ⚠ The neck is housing and not
lamp: where the body meets its base there has to be something holding it, and drawing that
in the lamp's own colour makes the light look as though it starts at the ceiling — the same nesting vanilla's beacon uses,
though the beacon reaches it with a cutout texture rather than a translucent one. ⚠ One
model cannot be part solid and part see-through, because the render type belongs to the
whole of it, so the two are separate models joined by the game's composite loader.

**And a form may be a different size on a wall than on a floor.** The fitting is a wide
bar where it is bolted to a wall and a small disc where it is set into a ceiling. ⚠ No
turning of one box gives both, so the size is asked for per face — `Shape.fit(facing)` —
and the model generator and the outline both ask that one question. Everything else
answers the same whichever face it is on and says so in one place rather than in each of
them. ⚠ Their masters are drawn without a border:
a one-pixel frame stretched onto a face six pixels across is a third of a pixel, which is
a smudge rather than a frame.

⚠ **And one form is mounted on nothing.** A rod runs through the middle of its cell from
one side to the other, so what it keeps is an axis rather than a facing — it is built like
the game's own chain, is the only form outside `PlateBlock`, and is the only one that does
not fall when what is beside it goes, because there is no face it was mounted on to lose.

**How a lamp is mounted is a different block, not a setting.** Everything that clings to
a face is a `PlateBlock` — it sits against one face of its own cell, holds water, and
takes its depth from the one number on `Shape`, read by the outline and by the box the
model is built from and by nothing else. What differs between them is which faces they can
sit on and how a click is read, and that is not a flag but a different set of block
states, so it is a subclass each: any of the six for the spotlight, up and down for the
slab and panel, the four walls for their vertical twins.

**Two slabs stack into a whole block, and it is still a slab.** Standing or lying. A cube of this mod wears
its lit face on all six sides; a stacked pair keeps the slab's own look — the face up and
down, the plain rim around the four cut sides — because that is what it is, and a
full-height course beside a single slab has to match. ⚠ It is also the one plate that
occludes: the others are declared `noOcclusion` because a thin thing is not a wall, but a
stacked pair is a whole block and a room walled with them would otherwise be lit straight
through. Only the slabs stack — two panels are eight pixels and not sixteen, and drawing
that as a whole block would be a lie about how much light is in it.

⚠ **Standing, the axis survives the stacking.** A pair of lying slabs is the same cube
whichever way you built it; a standing pair has its lit faces north and south, or east and
west, and those are not the same block to look at. So the facing is kept when the second
one goes in, and the whole block is drawn flat against the axis it was stacked along.

⚠ **That split came out of using it.** The slab first took the face you clicked, the way
the spotlight does, and it was wrong in the hand: clicking the side of a block gave a slab
standing against that side, where every player's hands expect one lying at the bottom or
the top of the cell — and an upper slab could only be had by finding a ceiling to click.
One block cannot be placed the way a slab is placed *and* the way a wall panel is placed,
because a click on a side has to mean one of the two. So each is placed the way its own
shape is expected to be, and one item on the bench turns either into the other.

Above that line nothing knows which property a plate keeps its direction in: the model
generator, the outline and the tests all ask it which way it faces. A fourth kind of
mounting would need no change outside its own class.

The ids carry `lamp` in front of them — `white_lamp_slab`, not `white_slab` — because a
`white_slab` reads as stone and a `white_panel` reads as a building block. Only the
spotlight, which is nothing else's name, stands alone.

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

Each shape has one recipe of its own, in white. The cube is glowstone in a frame of stone
around a pinch of redstone — or around a redstone torch, which is the inverted one, and is
the difference between the two in the world as well as on the bench. The spotlight is the
same glowstone in a ring of iron nuggets, which is the thing it is: a lens in a metal rim,
and cheap, because eight come out. The slab is a row of glowstone over a row of stone.

The panel is cut from the slab, the way the game cuts a block into slabs and by the same
arithmetic: three slabs of eight pixels are six panels of four.

The vertical forms have no recipe of their own at all — one of either turns into one of
its twin, in every colour and both ways. ⚠ **That is why nothing else in the mod is a
shapeless recipe of a single item.** Two such recipes taking the same one item are
ambiguous, and the game settles them by whichever it happened to load first; reserving
that shape of recipe for turning is what keeps every other one unmistakable.

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
- [x] **3** — slab and panel, each in two mountings: `PlateBlock` parameterised by depth
      with a subclass per mounting, the boxes derived from the direction rather than
      listed, a single item on the bench turning one mounting into the other, and two
      slabs stacking into a whole block that still looks like a slab, lying or standing
- [x] **4** — bulb, fitting and rod. ⚠ The rod turned out not to be a plate at all: it
      sits against no face, runs the length of its cell along an axis, and is built like
      the game's own chain. It is the one form outside `PlateBlock`
- [ ] **5** — a frame that is not the lamp's own colour: the cube, the slab and the panel
      again with the border fixed black, and again fixed white. Asked for on 2026-08-29.
      The master already keeps the frame and the face as separate numbers, so this is a
      second colour handed to the tint rather than a second set of shapes — but it is a
      third axis of the product, and how it reaches the recipes and the creative tab has
      to be decided before any of it is written
- [ ] **6** — each shape checked by game tests as it lands, rather than by eye

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
