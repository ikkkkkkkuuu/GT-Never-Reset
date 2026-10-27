# Compact AE machines from GT Not Good

Source: the user-supplied sibling `GT-Not-Good`, based on Git HEAD
`048fd1963de8c6458ef6f800c411be0087cbafdb`, including the local working changes
to QuantumComputer and AssemblerMatrix present on 2026-10-27.
SOURCE_MANIFEST.json records the actual copied source hashes.

The two controllers, dedicated ModularUI screens, CPU batching, matrix state,
quantum planner, cycle tools and attributed mixins derive from ABKQPO/GT-Not-Leisure
under LGPL-3.0-only. Existing source headers are retained. The earlier upstream
commit and file records are preserved in `../quantum-crafting-port/`.
GT Not Good additions retain its MIT license. Full LGPL, GPL and MIT texts are
included here.

Adaptations: use com.xyp.gtneverreset and gtneverreset resources, append machine
IDs 29449/29450 and packet IDs on the existing channel, use the native GregTech
base for the independent AE workflows, preserve the dedicated GUI controls while
using GT Never Reset's existing multiblock GUI theme and native textures,
split matrix inventory and GUI synchronization by responsibility, and consolidate
configuration in MainConfig through ConfigLoader. Preserve the source compact
structures, free power behavior, recipes, persistent tasks and native AE fluids.
The reference checkout is outside all compilation and resource source sets.
