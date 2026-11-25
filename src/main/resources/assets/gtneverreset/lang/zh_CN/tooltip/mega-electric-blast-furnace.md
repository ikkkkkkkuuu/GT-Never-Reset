每完成一个加工周期，炉温永久提高 {var:cycleHeat} K。
对于温度要求至少为 {var:discountHeat} K 的配方，炉温每高出要求 {var:discountHeat} K，基础耗电减半，每并行最低 1 EU/t。
每次超频使每刻耗电增为 4 倍。普通超频将加工时间减半，无损超频将加工时间缩短至四分之一。
并行上限等于炉温的开尔文数乘以 {var:parallelsPerKelvin}，再乘以线圈的并行倍率。
