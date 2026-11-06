# Universal Joint Factory port

Source project: [hongshaotimo/GTL-Enhancedcore](https://github.com/hongshaotimo/GTL-Enhancedcore).
Upstream implementation reviewed at `94d025ed7ebd48bfc071875b64201e7c51337838`.
Geometry was extracted from the user's installed `GTL-Enhancedcore-3.8.0.jar`;
exact archive/resource hashes and coordinate conversion are recorded in `SOURCE_MANIFEST.json`.

Upstream references:

- `common/machine/IntegratedUniversalFactoryMachine.java`
- `common/registration/IntegratedFactoryRegistration.java`
- `common/structure/IntegratedFactoryStructure.java`
- `data/gtl_enhancedcore/structures/integrated_universal_factory.pattern.gz`

The geometry is distributed under the upstream GNU LGPL v3, reproduced in `LICENSE.txt`.
The controller, recipe matching and GUI integration were implemented for Minecraft 1.7.10 / GTNH using GTNR's existing processing framework.
The upstream geometry is retained, converted to MBS1 and mapped to existing GTNH materials:

| Symbol | Upstream | GTNH replacement |
| --- | --- | --- |
| A, Q, X | Multi-functional casing / hatch panel | Solid Steel Machine Casing; only Q/X accept hatches |
| B | MV machine casing | MV Machine Casing |
| D | Solid machine casing | Solid Steel Machine Casing |
| E | Steel gearbox | Steel Gearbox Casing |
| F | Steel pipe casing | Steel Pipe Casing |
| G | Cupronickel coil | Cupronickel Coil Block |
| I | AE quartz glass | AE Quartz Glass |
| J | White lamp | Glowstone |

No new decorative blocks or textures are introduced. The controller uses GregTech's assembly-line overlay.
The source's 24 recipe types are mapped to their GTNH counterparts, including the single-block chemical dehydrator pool.
Balance changes requested by the user: 64 parallels per thread and 32 cross-recipe threads at LV;
each additional voltage tier doubles parallels and adds eight threads. Ordinary inputs run one selected recipe type.
Power, input transactions, output protection and persistence use the existing GTNR multiblock framework.
