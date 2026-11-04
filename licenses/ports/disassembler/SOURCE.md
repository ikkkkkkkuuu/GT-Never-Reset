# GregTech disassembler

The machine layout, base processing cost, tier checks, material recovery rules and exclusions are adapted from GTNewHorizons/GT5-Unofficial at the parent of the removal commit. The overlays are supplied at runtime by the installed GregTech mod. The historical high-tier machine definitions and crafting ingredients were checked against NewHorizonsCoreMod.

- [Original single-block machine, commit 32d0817](https://github.com/GTNewHorizons/GT5-Unofficial/blob/32d081720f26036f05559d4124ed0a51377c1f6f/src/main/java/gregtech/common/tileentities/machines/basic/GT_MetaTileEntity_Disassembler.java)
- [GregTech removal and LV through IV crafting recipes](https://github.com/GTNewHorizons/GT5-Unofficial/pull/1855/files)
- [NewHorizonsCoreMod removal and LuV through UMV crafting recipes](https://github.com/GTNewHorizons/NewHorizonsCoreMod/pull/556/files)
- [GoodGenerator exclusions](https://github.com/GTNH-Museum/GoodGenerator/pull/154/files) and [GT++ exclusions](https://github.com/GTNH-Museum/GTplusplus/pull/594/files)
- [Historical Chinese names and tooltip text](https://github.com/Kiwi233/Translation-of-GTNH/blob/a3211e446555ca109cc8a4b9edde52e038b48599/GregTech.lang) and the [matching English language file](https://github.com/Kiwi233/Translation-of-GTNH/blob/a3211e446555ca109cc8a4b9edde52e038b48599/GregTech_US.lang). The translation license is included as LICENSE-MIT-translations.txt.

GT-Never-Reset assigns its own meta-tile IDs and builds a recipe map for GTNH 2.9.0. Assembler routes take priority. Alternate routes refund their common recoverable ingredients in proportion to the original batch size. The crafting fallback reads registered GT machine recipes because the current GT recipe classes no longer retain the historical dismantleable flag.

The upstream GregTech license is included as `LICENSE-LGPL-3.0.txt`. The accompanying `LICENSE-GPL-3.0.txt` is also the license published by NewHorizonsCoreMod.
