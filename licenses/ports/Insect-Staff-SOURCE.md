# Insect Staff integration

The item integrates the GTNH forks resolved by the project's GTNH manifest:

- Draconic Evolution 1.5.35-GTNH: https://github.com/GTNewHorizons/Draconic-Evolution/tree/1.5.35-GTNH
- Ender IO 2.10.51: https://github.com/GTNewHorizons/EnderIO/tree/2.10.51

Draconic Evolution owns the staff model, original texture, GUI textures, configurable-tool profiles,
upgrade definitions and the delegated area-mining implementation. These assets are loaded from the
installed mod at runtime and are not copied into GTNR. GTNR applies a runtime insect palette and
adds its own layered armor plates, faceted amethyst core and emissive violet veins.
The MUI2 GUI follows GUIToolConfig's option list and separate numeric adjustment view,
ComponentFieldButton/ComponentFieldAdjuster's dimensions, texture regions and six step sizes,
and GUIToolInventory's slot layout. It loads the original DE/Brandon's Core GUI assets and vanilla
button textures at runtime, including the translucent enchantment-slot icons. It keeps one
configuration without the profile-name tab and extends the list for GTNR's additional settings.

Ender IO owns the advanced teleport staff targeting, travel packets, permissions and teleport
implementation. GTNR delegates right-click behavior and extends only item eligibility checks;
upstream distance, destination and travel-access checks remain in place.

GTNR's insect staff directly calls the existing VeinMiningPickaxe harvest implementation with its
GUI settings. Both tools share the synchronous vein search, matching, merged drops and deferred
ore backfill. The staff has no RF cost or durability; its destruction filter remains available.

GTNR owns the attack calculation, single-configuration GUI and dedicated key binding.
The item does not implement DE configurable-item or inventory-tool interfaces and is not selectable
in the DE configuration screen. No Draconic Evolution mixins are used.

Both mods remain external, optional integrations; the insect staff is registered when both are installed.
