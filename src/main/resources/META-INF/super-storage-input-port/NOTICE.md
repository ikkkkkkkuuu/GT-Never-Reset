# Super Advanced Stocking Input Bus (ME)

Ported from the adjacent GT-Not-Good project's SuperAdvancedMEInputBus and GUI.
GTNG's general MIT notice is included in GTNG-MIT.txt. These adapted sources
remain under LGPL-3.0, as specified by GTNG's original port notice, and are
excluded from GT-Never-Reset's general MIT grant.

Original reference: GTNewHorizons/GT5-Unofficial, tag 5.09.54.183.
Sources artifact SHA-1: b0f3eb42604cfc3ad88d56c50d0dbfa375d89419.
Source: https://github.com/GTNewHorizons/GT5-Unofficial/tree/5.09.54.183

Original adapted files:
- gregtech/common/tileentities/machines/MTEHatchInputBusME.java
- gregtech/common/gui/modularui/hatch/MTEHatchInputBusMEGui.java

Destinations under src/main/java/com/xyp/gtneverreset:
- common/machine/hatch/SuperAdvancedMEInputBus.java
- common/gui/modularui/SuperAdvancedMEInputBusGui.java

Retains 900 virtual item marks, per-item reserves and fixed available quantities,
ME recipe consumption, circuit/manual slots, world persistence and data-stick
copies. Pickup clears configuration, matching GTNG. GTNR adds cached ore-name
regex filtering of automatic selection, fail-closed invalid expressions,
filter persistence, linear lookup of surviving mark policies, and reorganized
paired grids with headers, a full-width filter field and centered settings.

The GPL-3.0 and LGPL-3.0 license texts are included. Native GregTech GUI and block
textures are referenced from the installed dependency; no artwork was copied.
Reference sources and QA runtime directories are not packaged.

Also ports GTNG SuperAdvancedMEInputHatch and SuperAdvancedMEInputHatchGui,
originally adapted from MTEHatchInputME.java and MTEHatchInputMEGui.java.
Destinations: common/machine/hatch/SuperAdvancedMEInputHatch.java and
common/gui/modularui/SuperAdvancedMEInputHatchGui.java. The fluid variant retains
900 fluid marks and per-fluid policies, with labelled paired grids and centered
settings. Fluids do not use the item ore dictionary filter.
GTNR fluid auto-selection additionally supports cached regular expressions over registered fluid names, with persisted rules and invalid-expression feedback.
