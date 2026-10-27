# GT-Not-Good Vault port

Adapted from the adjacent GT-Not-Good working tree, HEAD
048fd1963de8c6458ef6f800c411be0087cbafdb:
https://github.com/ikkkkkkkuuu/GT-Not-Good

The source contains local changes beyond HEAD. SOURCE_MANIFEST.json pins
the exact controller, port, GUI, API, storage helpers, structure and resources.
GT-Not-Good's MIT grant is included as LICENSE-GTNG-MIT.txt. GregTech / GT++
contributors retain their LGPL-3.0-or-later rights in the inherited machine,
rendering and AE integration implementations. Full LGPL/GPL texts are included.

The Eddington casing, Kerr Compressor overlays and animation metadata are
copied unchanged from GTNG's Beyond the Stars artwork port. Original author:
V3rySp3d, copyright 2026, MIT license; see LICENSE-Beyond-the-Stars-MIT.txt.
Upstream: https://github.com/v3rysp3d/GTNH-Beyond-the-Stars . The supplied
upstream snapshot has no Git metadata, so no upstream revision is claimed.
The 47 Eddington connected tiles are GTNG's MIT-derived artwork. Only this
one casing and the Vault's overlays are included, along with the GTNG GUI
and port icon resources. These notices do not license unrelated assets.

The port retains GTNG's 3x2x2 structure, exactly one Vault Port Hatch,
shared item/fluid byte capacity, per-type long counts, zero EU operation,
the port's infinite AE power, specialized 344x232 storage GUI and Eddington
connected textures. Original GTNG recipes use native GTNH materials and
are retained. Registrations and resources use the gtneverreset domain.
No GTNG or GTNL runtime dependency is added.
