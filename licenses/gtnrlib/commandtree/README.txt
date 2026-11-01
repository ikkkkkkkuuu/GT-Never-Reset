Source: local GT-Not-Good commandtree implementation (snapshot 2026-11-01; checkout has no Git metadata).
Upstream: https://github.com/EmoUsedHM01/brigadier-tree-gtnh
Pinned upstream commit: fb8e6dc7c4c04b10139e75bc260974f38d8c4e95
Licenses: MIT; Brigadier (Microsoft), Brigadier Tree GTNH (EmoUsedHM01), commodore-file (lucko and contributors). See accompanying license texts.

Destinations: com/xyp/gtnrlib/command/{brigadier,commodore,tree}, client/{command,gui/command}, network, mixins/early/command; assets/gtnrlib/commandtree.
Changes: relocated packages/resources and Mixin members; library-owned lifecycle/network; client side isolation; merged GUI-local helpers, argument serializers and definition registration wrappers; project formatting; retained permission filtering and vanilla completion fallback.
