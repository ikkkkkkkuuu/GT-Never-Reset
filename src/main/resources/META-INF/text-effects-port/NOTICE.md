# Tooltip text effect attribution

The text renderer, layout/parser, mask cache, font compatibility code and shader
support are adapted from ABKQPO/GT-Not-Leisure, local dev-290 reference snapshot:
https://github.com/ABKQPO/GT-Not-Leisure

Upstream LICENSE.txt grants LGPL-3.0. The LGPL and accompanying GPL texts are
included here. The Exotic Rainbow shader is copied from the adjacent
GT-Not-Good project's assets/gtnotgood/shaders/text/exotic_rainbow.frag.glsl.
That project attributes its shader port to GTNL revision
52345d2c059c871febec1365d6012424c7d64547. The upstream shader license notices
for Calamity Overhaul and Fargo Team are included here.

GT Never Reset changes: package/resource namespace, client-only integration,
GTNR-scoped dispatch, and selection of only the Exotic Rainbow preset for the
existing localized adder tooltip. Palette, normal font weight and speed match
GTNG's saved machinecredit configuration on 2026-10-27. Only a fixed internal
adder marker is accepted; configurable inline styles, preferences, other presets
and preview commands are not included. The local GTNL reference has no Git metadata.
