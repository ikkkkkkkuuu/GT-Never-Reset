首次杂交得到的品种以 1/1/1 入档，再次产种时沿用档案属性
洗种模式每 tick 消耗 {white:{var:washUU} L UU 物质}，功耗 {white:{var:baseEu} EU/t}
每 {white:{var:washSeconds} 秒}尝试将一项未满 31 的属性提高 1 点
所需底土放入{white:输入总线}，{green:不会消耗}；原版底土无需放入

杂交等级为 1–5 级，对应电压为 {yellow:HV} / {dark_gray:EV} / {blue:IV} / {light_purple:LuV} / {aqua:ZPM}，GUI 中可查看
1 级每轮 {white:{var:firstSeconds} 秒}，每升一级耗时翻倍；超过所需电压后执行有损超频
默认 1 并行，从 {yellow:HV} 起每升{gold:两级电压}，并行上限翻倍
每并行基础耗电 {white:{var:baseEu} EU/t}，每轮消耗 {white:{var:fertilizer} L 富集肥料}

1–5 级默认成功率为 100% / 80% / 60% / 40% / 20%，各并行独立判定
杂交失败也会消耗肥料和催化剂；底土始终保留
