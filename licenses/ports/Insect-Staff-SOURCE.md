# Insect Staff integration

The item integrates the GTNH forks resolved by the project's GTNH manifest:

- Draconic Evolution 1.5.35-GTNH: https://github.com/GTNewHorizons/Draconic-Evolution/tree/1.5.35-GTNH
- Ender IO 2.10.51: https://github.com/GTNewHorizons/EnderIO/tree/2.10.51

Draconic Evolution owns the staff model, original texture, GUI textures, configurable-tool profiles,
upgrade definitions and the delegated area-mining implementation. These assets are loaded from the
installed mod at runtime and are not copied into GTNR. GTNR applies a runtime insect palette and
adds its own armor plates and emissive eyes. The MUI2 settings panel references the installed
Draconic Evolution GUI textures and retains its field names, modifiers and six step sizes.

Ender IO owns the advanced teleport staff targeting, travel packets, permissions and teleport
implementation. GTNR delegates right-click behavior and extends only item eligibility checks;
upstream distance, destination and travel-access checks remain in place.

GTNR's vein matching follows its existing VeinMiningPickaxe conventions. The new staff scheduler
uses the server's normal harvest operation, preserves Forge break/harvest events and processes
bounded batches instead of running the entire vein search in one tick.

GTNR owns the attack calculation, single-configuration GUI and dedicated key binding.
The item does not implement DE configurable-item or inventory-tool interfaces and is not selectable
in the DE configuration screen. No Draconic Evolution mixins are used.

Both mods remain external, optional integrations; the insect staff is registered when both are installed.
