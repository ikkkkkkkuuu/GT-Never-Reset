# Radial menu

Ported from the adjacent GT-Not-Good implementation of Navatusein's
GTNH-Radial-Menu, revision `6fc34b6904cf66b7aa818025542bb61a550a9312` (MIT).
The original MIT notice, Phosphor icon notice, and LuaJ notices are included here.
See `ASSET_MANIFEST.json` for the upstream resources, hashes, and class mapping.

GT-Never-Reset changes the host namespace, organizes classes by responsibility,
keeps private editor helpers with their GUI, and integrates client preferences
into `MainConfig` and initialization into the existing loaders. Tool belt state
and server operations live in `utils.inventory`; its packets use the host channel
and synchronize the owner's inventory. Existing profile JSON and belt NBT keys
remain compatible. LuaJ 3.0.1 is bundled and relocated by the host build.
