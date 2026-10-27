# GT-Never-Reset IO devices

Direct source: GT-Not-Good, commit 048fd1963de8c6458ef6f800c411be0087cbafdb,
https://github.com/ikkkkkkkuuu/GT-Not-Good . Original MIT grant: GTNG-MIT.txt.
The imported IO source files matched that revision. Upstream-derived code and
artwork keep the license terms recorded in the adjacent original notices and
META-INF/advancedio-port, which are preserved as source provenance.

Current destinations are under com.xyp.gtneverreset:
- common.part: PartAdvancedIOBus and PartStockIOInterface;
- common.item.items: ItemAdvancedIOBus and ItemStockIOInterface;
- common.block.blocks, its item and tile subpackages: the stock interface block;
- common.gui.modularui and its handler, sync and widget subpackages: both GUIs;
- utils.machine.advancedio and utils.machine.stockio: transfers and transactions;
- mixins.late.gregtech: native singleblock recipe and generator integrations;
- common.recipe.ae2 and loader: original recipes and local registration.

Adaptations: local packages, registry domain and GUI ownership; independent GUI
support classes; the full-block stock interface uses a concrete AE network tile
with the source node lifecycle and owner persistence. Ghost filters, reserves,
fixed amounts, server authorization, real-consumption settlement, refund escrow,
removal drops and lossless block/part crafting conversion are retained.
No super advanced GregTech input bus or threshold export bus is registered.
META-INF/super-storage-input-port is retained only as provenance and license
text for the reserve/fixed behavior inherited by StockIOLogic.

Advanced IO block and GUI textures are copied byte-for-byte; their destination
resource domain is assets/gtneverreset. The asset manifest records the original
sources and hashes. Installed GregTech/AE2 textures are referenced at runtime.
The original LGPL/GPL/CC BY-NC-SA notices remain applicable; this port does not
replace those grants with the project's general license.
