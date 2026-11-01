Plans deterministic CropsNH mutations with two to four parents and multi-step routes
Consumes one analyzed CropsNH seed per new species to permanently archive its species and stats
Archived parents are reusable; additional seeds of the same species do not replace archived stats
Successful intermediate crops are archived and output as one seed; the target is bred repeatedly
GUI tabs: operation, parent archive, breedable crops and route; supports search and NEI target drops
The route tab shows a parent lineage graph; drag or scroll to pan, Ctrl + scroll to zoom, middle-click to reset
Breedable means reachable from the parents; voltage, fertilizer and catalysts are still required
Uses deterministic machine mutations only, excluding world-only mutations and random mutation pools

Preserves recipe voltage, base duration and EU/t; each overclock uses four times the EU/t and half the time
Success chance depends on the resulting crop, independently of voltage; the GUI shows chances for crops and route steps
Default crop tiers 1–3 / 4–6 / 7–9 / 10–12 / 13–15 / 16+ have 100% / 80% / 60% / 40% / 20% / 10% chances
Configure tier bands and per-crop ID overrides under crop_breeder in machine.cfg; restart required
Failed attempts still consume catalysts and fertilizer but retain substrate, without producing seeds or unlocking crops
Substrate is checked in input buses without consumption and can be reused; supply one alternative from every group
Without an extra substrate requirement, use one of the crop's planting soils; the GUI cycles through the alternatives
Other recipe catalysts are still consumed; input hatches supply enriched fertilizer
Uses the CropsNH fertilizer formula; output stats are the rounded-down averages of all parent stats
Honors the mod's existing CropsNH max-stats setting; when enabled, seed stats remain fixed at 31/31/31
Will not start while the output is full and waits if it fills before completion; saves active work across chunk unloads
No parallel, batch or parameter module support
