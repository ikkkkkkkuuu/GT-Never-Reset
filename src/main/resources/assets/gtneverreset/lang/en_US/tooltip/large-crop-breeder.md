{gold:Automatic crop breeding, without the manual crossbreeding}
Choose a target; the machine breeds recorded parents through the entire route
Insert analyzed seeds to record their species; one seed is consumed for each new species
Newly bred species start at 1/1/1. Subsequent production uses their archived stats
Switch modes on the operation tab to improve an archived crop. It uses {white:{var:washUU} L of UU-Matter} per tick and pauses when supply runs out
Each {white:{var:washSeconds}-second} cycle uses {white:{var:baseEu} EU/t} and raises one random stat below 31 by one
Place required substrates in an {white:input bus}; they are {green:not consumed}. Vanilla substrates need no item

Breeding grades 1–5 require HV / EV / IV / LuV / ZPM; check the GUI for each crop
Grade 1 takes {white:{var:firstSeconds} seconds}; each grade doubles the time. Higher voltage applies lossy overclocks
Starts with 1 parallel at HV; every {gold:two voltage tiers} doubles the parallel limit
Each parallel starts at {white:{var:baseEu} EU/t} and consumes {white:{var:fertilizer} L enriched fertilizer} per attempt

Default chances for grades 1–5 are 100% / 80% / 60% / 40% / 20%; each parallel rolls independently
Failed attempts still consume fertilizer and catalysts; substrates are always retained
