# Pattern input assemblies

Adapted from GT-Not-Good revision
`048fd1963de8c6458ef6f800c411be0087cbafdb`:
https://github.com/ikkkkkkkuuu/GT-Not-Good

Copyright (c) 2026 ChatGPT. The GT-Not-Good MIT grant is reproduced in
`GTNG-MIT.txt`. Source paths and original SHA-256 hashes are recorded in
`SOURCE_MANIFEST.json`.

The super pattern input implementation builds on GregTech's ME crafting input
and output implementations. GregTech / GT5-Unofficial contributors retain their
LGPL-3.0-or-later rights in that material. The LGPL-3.0 and incorporated GPL-3.0
license texts are included beside this notice. These upstream terms are not
replaced by GT-Not-Good's MIT grant.

The port keeps the circuit and super pattern assemblies, their automatic circuit
and reusable mold handling, native ME output, and interface terminal names. Both
assemblies have 540 pattern slots, shared through their respective input mirrors.
Code is arranged by responsibility under
`com.xyp.gtneverreset`; translation and registration keys use `gtneverreset`.
Wildcard pattern items are outside this port. The GUI uses
installed GregTech textures directly; no copied bitmap assets are included.
