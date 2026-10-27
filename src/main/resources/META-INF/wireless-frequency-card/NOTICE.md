# Wireless Frequency Card

Adapted from the GT-Not-Good working tree at revision
`048fd1963de8c6458ef6f800c411be0087cbafdb`:
https://github.com/ikkkkkkkuuu/GT-Not-Good.
`SOURCE_MANIFEST.json` records the exact source files and SHA-256 hashes.

The original wireless implementation comes from GT-Not-Leisure PR 627,
commit `651396c75d108e6ff5399a44c56c12214ad27d3d`:
https://github.com/ABKQPO/GT-Not-Leisure.
The adapted Java implementation and associated mixins retain LGPL-3.0-only,
outside the project's general license grant. The full license is bundled as
`LICENSE-LGPL-3.0.txt` beside this notice.

The source snapshot includes the later GTNL topology and channel allocation
optimizations identified by commits `3e2d43da1260507a29fe674eb90dc8e99553bf83`,
`0730f502bbe1f3f48e27699ab8085ee8c0d58929`,
`1ea3990f6d6c3b02fc09e5459e48c2b93c5f357e`,
`27712ce26fd045526768fef396b16fe57ec56dcc` and
`831f06ceb172025a91a55ca00deca7c21ea4a6f0`.
It also preserves GTNG's multiple saved sources, per-source automatic connection
preferences, machine grouping, renaming and network visualization.

GT-Never-Reset adapts the packages, configuration, registration, translations,
network transport and mixin prefixes. GUI, container, packet, world and machine
support responsibilities live in their corresponding packages. Client screens,
panels and presentation live under `client.gui`, following the upstream
client-side separation. Common GUI entry, synchronization and server view state
remain separate. Layout and snapshot types are nested in their owning classes;
localization and server view-writing helpers stay with their respective consumers.
GTNG-only device types are omitted; native AE2 and GregTech support is retained.
Tile persistence is registered as an early mixin. Existing NBT keys are retained.
Reference sources are not compiled or packaged. No upstream endorsement is implied.

Matter Manipulator compatibility, copied-link authorization and queue budgets,
and stable card identity tracking are adapted from GT-Not-Leisure PR 655,
commit `81cd514129dbb2ce06fdea2d8d3cbdbeb14b05d8`:
https://github.com/ABKQPO/GT-Not-Leisure/pull/655.
These changes and the adapted compatibility checks retain LGPL-3.0-only.
`SOURCE_MANIFEST.json` records the upstream commit and source Git blob identities.
The local multiple saved sources, names and per-source automatic preferences are
preserved. Copy restoration requires the card's currently selected frequency;
historical saved sources do not grant restoration permission.
Matter Manipulator is an optional integration, compiled against the project's
fixed GTNH manifest and loaded only when Matter Manipulator and AE2 are present.

The unchanged item texture comes from AE2 Lightning Tech Reborn,
commit `2d8369e1e8b952300ec7ee73837e75cb61c7fc8d`:
https://github.com/AE2-Lightning-Tech-Reborn/AE2-Lightning-Tech-Reborn.
It retains CC BY-NC-SA 3.0, including attribution, noncommercial and ShareAlike
requirements, separately from the Java license. The original attribution is in
`GTNL_ASSET_NOTICE.txt`; the complete license is in `LICENSE-CC-BY-NC-SA-3.0.txt`.
`ASSET_MANIFEST.json` records its upstream provenance, destination and SHA-256 hash.
