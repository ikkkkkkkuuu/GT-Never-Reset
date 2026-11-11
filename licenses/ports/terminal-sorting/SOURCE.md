# Wireless terminal default sorting tree

- Resource: `assets/gtneverreset/terminal/InvTweaksTree.txt`
- Source: GT New Horizons modpack, `config/AppliedEnergistics2/InvTweaksTree.txt`
- Revision: `af8d03090df7cc3c060374352412da3ca9022ae9`
- URL: https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/af8d03090df7cc3c060374352412da3ca9022ae9/config/AppliedEnergistics2/InvTweaksTree.txt
- Authors credited by the original file: Jimeo Wan (Inventory Tweaks), Larandar and Lunaqua; subsequent contributions by the GT New Horizons project.
- License: Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International, as distributed by the source repository. See `LICENSE-CC-BY-NC-SA-4.0.txt`. This adapted resource remains under that license.
- Changes: remove the Inventory Tweaks-only `treeVersion` attribute so AE2 recognizes the root category; qualify vanilla item IDs with `minecraft:` to match AE2's item registry lookups; move source credits here and remove the original Inventory Tweaks-specific usage comments. Category and item ordering, damage values and ore dictionary rules are preserved.

GTNH removed the source configuration in commit `e658a2baeff4e99f64c31ae5ee8ef6007b1ac4e6` (PR #27253, 2026-10-13), while AE2 rv3-beta-1095-GTNH still loads it from `config/AppliedEnergistics2/InvTweaksTree.txt`.

GTNR bundles this fallback and installs it only when that AE2 file is absent and no sorting tree is already loaded, when the wireless terminal is first opened. Existing files are never replaced, merged or normalized. AE2's public initializer and comparator are reused; no separate sorter or Inventory Tweaks mod is required. The installed tree is shared by AE2 terminals. Changes to the on-disk file after initialization require a client restart, as with AE2's normal configuration loading.

These source and license records are distributed through the existing `licenses/ports` resource and source-JAR tasks.
