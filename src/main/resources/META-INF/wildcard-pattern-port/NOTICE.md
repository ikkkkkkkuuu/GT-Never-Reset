# Wildcard Pattern port

Adapted from the local GT-Not-Good source tree at revision
`048fd1963de8c6458ef6f800c411be0087cbafdb`:
https://github.com/ikkkkkkkuuu/GT-Not-Good

GT-Not-Good's MIT license, copyright (c) 2026 ChatGPT, is included in
`GTNG-MIT.txt`. Source paths and SHA-256 hashes are recorded in
`SOURCE_MANIFEST.json`; these hashes identify the actual local inputs.
Wildcard-Pattern 0.1.2 by LeoDreamer supplies the upstream interaction contracts;
its MIT license is included in `Wildcard-Pattern-MIT.txt`.

The port retains material expansion, filtering, exclusions, deduplication,
preview, NEI recipe import, matrix multiplication and AE/GT crafting integration.
Classes are arranged by responsibility under `com.xyp.gtneverreset`. The item is
registered in this mod's namespace; existing wildcard NBT keys are retained.
The prototype LDLib screen and command are excluded. The editor uses native
ModularUI2 widgets and texture rendering instead of importing the LDLib runtime.
The small font scope and screen adapter preserve bitmap Latin text and Unicode
fallback. LDLib's applicable LGPL-3.0 license is included in `LDLib-LGPL-3.0.txt`.

Copied artwork keeps its original bytes. Entries and hashes are in the manifest.
The following images are under `assets/gtneverreset/textures/gui/ldlib/modern/`:

- AE2 contributors, commit `fd8b717a405672ce4f65ba540f1db8c91317daa4`:
  `background.png`, `text_field.png`, `button.png`, `button_highlighted.png`,
  `button_disabled.png`. LGPL-3.0; see `AE2-LGPL-3.0.txt`.
  https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/fd8b717a405672ce4f65ba540f1db8c91317daa4
- ModernityGTNH contributors, commit `87fee0aac305ca8f1736278266ad87d94deff3b4`:
  `slot.png`, `tabs_left.png`. CC BY-NC-SA 4.0; see
  `Modernity-CC-BY-NC-SA-4.0.txt`.
  https://github.com/ModernityGTNH/Modernity-GTNH-UI/tree/87fee0aac305ca8f1736278266ad87d94deff3b4
- GregTechCEu contributors, v7.4.0-1.20.1, commit
  `07ac5207f6f58ba3a2f9cd8b862b382d24300a17`: `close.png` and
  `progress_bar_arrow.png`. LGPL-3.0; see `GTCEu-LGPL-3.0.txt`.
  https://github.com/GregTechCEu/GregTech-Modern/tree/v7.4.0-1.20.1

The included GPL-3.0 text accompanies the LGPL-3.0 grants. Original authors retain
their respective rights; these terms are not replaced by the host's license.
