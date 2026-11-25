线圈决定基础炉温。能源仓的合计电压每比 {aqua:LV} 高一级，基础炉温就多 {red:{var:voltageHeat} K}。
每完成一批，炉温永久提高 {red:{var:cycleHeat} K}。当前炉温可用扫描仪查看。
炉温达标才能开工。总供电须达到配方标注的 EU/t，温差折扣随后再算。
{hr}
配方要求至少 {red:{var:discountHeat} K} 时，炉温每高出要求 {red:{var:discountHeat} K}，基础耗电就{aqua:减半}。
每并行在超频前至少消耗 {aqua:{var:minEUt} EU/t}。
炉温每高出配方要求 {red:{var:perfectOverclockHeat} K}，就有一次普通超频变为{light_purple:无损超频}。
{hr}
用白铜线圈和 {aqua:LV} 供电，刚成型时的理论并行上限是 {gold:{var:baseParallels}}。
理论并行上限为 {gold:{var:parallelsPerKelvin}} × 炉温(K) × 线圈倍率。实际能跑多少取决于供电、原料和输出余量。
{light_purple:跨配方 ME 样板仓}可同时加工两种配方池的样板。普通输入按机器界面选定的模式加工。
工业高炉模式按 NEI 配方放电路，部分配方通保护气烧得更快。
