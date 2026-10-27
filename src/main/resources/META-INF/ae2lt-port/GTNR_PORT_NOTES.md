# GT-Never-Reset packaged provider port

Ported on 2026-10-27 from the adjacent GT-Not-Good source snapshot. That checkout
has no Git metadata; SOURCE_MANIFEST.json records the exact source hashes and
destination paths instead of attributing an unverified commit. Upstream pinned
revisions, authors and licenses remain in NOTICE.md, CODE_PORT_NOTES.md and the
accompanying license texts. Those documents describe the original GTNG port.

The packaged provider implementation and its GTNR adaptations remain
LGPL-3.0-only. They are excluded from the project's general MIT grant.
TileEntityAENetworkBase derives from GTNG's MIT-licensed TileMEBridgeBase;
GTNG-MIT-LICENSE.txt retains that grant. The opt-in QA fixture derives from
GTNG's MIT-licensed PackagedProviderClientChecks and is not packaged in the mod.

GTNR modifications:

- Move block, tile, items, GUI, rendering, recipes, adapters and extension
  interfaces into their respective responsibility packages. Encapsulate tile
  state accessed by the GUI and machine adapters. Reuse the existing arcane
  pattern layout and fluid compatibility helpers.
- Register items, the block, recipes, renderers and conditional mixins through
  existing loaders. Append the connector request to NetWorkHandler's existing
  channel, preserving earlier packet IDs and server-thread dispatch. Reject
  queued requests when the player changes world or disconnects.
- Use the gtneverreset resource/registry namespace, GTNR item and creative-tab
  registries and MainConfig's packaged-provider category. Preserve saved NBT
  keys, core adapter IDs, item/block path names and original interactions.
- Preserve 720 patterns, nine returns, core locking, same-dimension loaded
  targets, owner/research checks, real machine processing and exact-result
  collection. Include infusion, crucible, arcane workbench, blood altar,
  assembly line, advanced assembly line and circuit assembly line adapters.
- Use flat dark-gray text without shadow on provider labels, target rows and priority controls, as requested.
- Keep the selected original images unchanged. ASSET_MANIFEST.json records
  their GTNR destinations and original hashes. CC-BY-NC-SA assets retain their
  separate noncommercial/share-alike license; they are not MIT artwork.

Distributors must supply matching corresponding source for the LGPL-covered
implementation and preserve these notices. The Gradle sourcesJar task includes
the editable Java sources. Reference checkouts and QA fixtures do not belong in
the binary release JAR.
