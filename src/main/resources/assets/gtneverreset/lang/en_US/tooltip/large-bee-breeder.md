{gold:Automatically breeds bees to meet your bee needs}
Select a breeding target in the controller; the machine automatically breeds recorded species to unlock it.
The default success chance is the species' base mutation chance.
Insert a princess, queen or drone to store a specimen of its species. Registration leaves the bee in the input bus.
Bee foundations are disabled by default; when enabled in config, place them in an {white:input bus}; they are {green:not consumed}
Bee species have seven tiers from HV to UHV; the GUI shows the required voltage

Each cycle takes {white:{var:attemptSeconds} seconds} by default
Each parallel consumes {white:{var:baseEu} EU/t}
Higher voltage applies lossy overclocks.
Each parallel consumes 2 royal jelly for one extra lossy overclock.
Starting from {yellow:HV} glass, each higher glass tier {red:doubles} EU/t and adds {gold:+{var:glassChance}%} success chance
Breeding uses a matching princess or queen and a drone. The machine prepares missing parents from stored specimens and keeps intermediate offspring for later steps.
On success, the female has a {var:femaleRetentionChance}% chance to survive and the drone has a {var:droneConsumptionChance}% chance to be consumed. On failure both parents remain available.
Automatic mode breeds the target and then exports princesses. Drone and Princess modes export stored species directly.
Each prepared or exported bee takes {white:{var:seedSeconds} seconds} and requires its species voltage. These cycles have no overclocking or parallels.
Target offspring and exports are purebred with fully enhanced genes and retain the target species' original special effect.
Parameter module changes apply to the next cycle.
Parallel parameters multiply voltage-based parallel capacity; overclock parameters also apply to the extra royal jelly overclock.
