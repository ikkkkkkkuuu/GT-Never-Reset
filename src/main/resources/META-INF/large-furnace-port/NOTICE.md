# Large Furnace port

Adapted from the adjacent GT-Not-Good working tree, HEAD
048fd1963de8c6458ef6f800c411be0087cbafdb. The controller has local changes beyond
that revision; SOURCE_MANIFEST.json identifies the exact source contents.

The structure and controller originate from S_Fast's GTL-Enhancedcore 3.8.0,
under LGPL-3.0. The two controller overlays originate from GregTechCEu /
GregTech-Modern 1.4.4, under LGPL-3.0. The GTNG controller and structure carry
LGPL-3.0-only notices. Its per-input furnace lookup carries GPL-3.0-only.
The controller recipe and NEI registration are covered by GTNG's MIT grant.
Complete license texts are included in this directory. These third-party
portions are outside GT-Never-Reset's general MIT grant.

The overlays preserve the adjacent project's current PNG bytes: 62 frame pixels
per 16x16 image are transparent so GTNH's separately rendered steel casing shows
through; the remaining 194 pixels retain the upstream panel and indicators.
ASSET_MANIFEST.json records the original archives and source/output hashes.
No matching Enhancedcore source revision was established by the source port;
the installed archive hashes identify those upstream versions.

The 11 x 13 x 12 structure matrix retains 716 non-space cells, the controller
offset (5,11,0), native steel blocks/casings, 64 Coke Oven Bricks, 102 Firebricks,
iron bars and 56 stationary lava blocks. The original lava block matching is
retained, without an additional fluid-level check. Its final depth row is entirely spaces;
the occupied bounds reported by the automatic Shift tooltip are 11 x 13 x 11.
Spaces remain unconstrained. The original pattern is included for provenance
and is not loaded as a game resource.

At the user's request, this adaptation uses GTNRMultiBlockBase, its existing
GUI and parameter modules. It replaces GTNG's shared batch processing and
special parallel/thread control hatches with independently progressing recipe
threads sharing the available machine power. At LV it has 64 base parallels
and 6 recipe threads; both double per voltage tier. Base speed is 200% and
base EU consumption is 80%. Parameter modules apply the existing GTNR rules.
The furnace lookup examines distinct input types to avoid GTNH's first-input
furnace lookup blocking later smeltable inputs. GTNG's dedicated batch GUI,
wireless output helpers, parallel hatches and Omega thread infrastructure are
not included. Controller ingredients and native NEI smelting integration are
retained, with a separate GTNR registry ID and resource domain.
