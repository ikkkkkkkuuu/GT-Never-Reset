package com.xyp.gtneverreset.utils.enums;

/** Server-synchronized adapter feedback shown in the original core-slot tooltip. */
public enum AltarStatus {

    // #tr gtnr.gui.packaged.altar_idle
    // # Altar: idle
    // # zh_CN 祭坛：空闲
    Idle("gtnr.gui.packaged.altar_idle"),
    // #tr gtnr.gui.packaged.altar_running
    // # Altar: active job or waiting for return
    // # zh_CN 祭坛：任务进行中或等待回收
    Running("gtnr.gui.packaged.altar_running"),
    // #tr gtnr.gui.packaged.altar_essentia
    // # Altar: waiting for AE essentia, power or channel
    // # zh_CN 祭坛：等待 AE 源质、电力或频道
    Essentia("gtnr.gui.packaged.altar_essentia"),
    // #tr gtnr.gui.packaged.altar_inactive
    // # Altar: activate the structure with a wand first
    // # zh_CN 祭坛：请先使用法杖激活结构
    Inactive("gtnr.gui.packaged.altar_inactive"),
    // #tr gtnr.gui.packaged.altar_owner
    // # Altar: owner must be online in this dimension
    // # zh_CN 祭坛：放置者须在本维度在线
    Owner("gtnr.gui.packaged.altar_owner"),
    // #tr gtnr.gui.packaged.altar_unloaded
    // # Altar: waiting for loaded chunks
    // # zh_CN 祭坛：等待区块加载
    Unloaded("gtnr.gui.packaged.altar_unloaded"),
    // #tr gtnr.gui.packaged.altar_occupied
    // # Altar: structure or pedestal contents prevent dispatch
    // # zh_CN 祭坛：结构或基座物品阻止派单
    Occupied("gtnr.gui.packaged.altar_occupied"),
    // #tr gtnr.gui.packaged.altar_recipe
    // # Altar: check research, ingredients and the encoded output
    // # zh_CN 祭坛：请检查研究、材料与样板产物
    Recipe("gtnr.gui.packaged.altar_recipe"),
    // #tr gtnr.gui.packaged.altar_interrupted
    // # Altar stopped without the expected result. Inspect the altar before releasing its job.
    // # zh_CN 祭坛已停止且缺少预期产物，检查祭坛后再释放任务。
    Interrupted("gtnr.gui.packaged.altar_interrupted"),
    // #tr gtnr.gui.packaged.returns_full
    // # Return slots full. The result remains in the altar.
    // # zh_CN 回收槽已满，产物保留在祭坛中。
    ReturnsFull("gtnr.gui.packaged.returns_full"),
    // #tr gtnr.gui.packaged.blood_recipe
    // # Blood Altar: check tier, single-step ingredients and output.
    // # zh_CN 血祭坛：请检查等级、单步配方材料与产物。
    BloodRecipe("gtnr.gui.packaged.blood_recipe"),
    // #tr gtnr.gui.packaged.arcane_owner
    // # Workbench: owner must be online here and allowed to use the target.
    // # zh_CN 工作台：所有者须在本维度在线且有权使用目标。
    ArcaneOwner("gtnr.gui.packaged.arcane_owner"),
    // #tr gtnr.gui.packaged.arcane_occupied
    // # Workbench: clear the grid and close its screen.
    // # zh_CN 工作台：请清空九宫格并关闭其界面。
    ArcaneOccupied("gtnr.gui.packaged.arcane_occupied"),
    // #tr gtnr.gui.packaged.arcane_wand
    // # Workbench: insert a crafting wand or sceptre.
    // # zh_CN 工作台：请放入可合成的法杖或权杖。
    ArcaneWand("gtnr.gui.packaged.arcane_wand"),
    // #tr gtnr.gui.packaged.arcane_recipe
    // # Workbench: check recorded grid, research, exact inputs and output.
    // # zh_CN 工作台：请检查录入的九宫格、研究、材料及产物。
    ArcaneRecipe("gtnr.gui.packaged.arcane_recipe"),
    // #tr gtnr.gui.packaged.arcane_capacity
    // # Workbench: the recipe exceeds this wand's capacity.
    // # zh_CN 工作台：配方所需 Vis 超过法杖容量。
    ArcaneCapacity("gtnr.gui.packaged.arcane_capacity"),
    // #tr gtnr.gui.packaged.arcane_essentia
    // # Workbench: waiting for matching AE primal essentia (Thaumic Energistics).
    // # zh_CN 工作台：等待 AE 中对应的基础源质（需要神秘能源）。
    ArcaneEssentia("gtnr.gui.packaged.arcane_essentia"),
    // #tr gtnr.gui.packaged.arcane_returns_full
    // # Workbench: make room in the provider's return inventory.
    // # zh_CN 工作台：请腾出供应器回收栏空间。
    ArcaneReturnsFull("gtnr.gui.packaged.arcane_returns_full"),
    // #tr gtnr.gui.packaged.arcane_ready
    // # Workbench: crafting complete; results are waiting to enter ME.
    // # zh_CN 工作台：合成已完成，产物等待送入 ME。
    ArcaneReady("gtnr.gui.packaged.arcane_ready"),
    // #tr gtnr.gui.packaged.crucible_owner
    // # Crucible: owner must be online here and allowed to use the target.
    // # zh_CN 坩埚：所有者须在本维度在线且有权使用目标。
    CrucibleOwner("gtnr.gui.packaged.crucible_owner"),
    // #tr gtnr.gui.packaged.crucible_heat
    // # Crucible: heat above 150, or place a heat source below a dry crucible.
    // # zh_CN 坩埚：温度须高于 150；无水时可直接在下方放置热源。
    CrucibleHeat("gtnr.gui.packaged.crucible_heat"),
    // #tr gtnr.gui.packaged.crucible_recipe
    // # Crucible: check research, one catalyst, output and competing local aspects.
    // # zh_CN 坩埚：请检查研究、单个催化剂、产物和已有源质的配方冲突。
    CrucibleRecipe("gtnr.gui.packaged.crucible_recipe"),
    // #tr gtnr.gui.packaged.crucible_essentia
    // # Crucible: waiting for missing AE essentia (Thaumic Energistics).
    // # zh_CN 坩埚：等待 AE 中缺少的源质（需要神秘能源）。
    CrucibleEssentia("gtnr.gui.packaged.crucible_essentia"),
    // #tr gtnr.gui.packaged.crucible_returns_full
    // # Crucible: make room in the provider's return inventory.
    // # zh_CN 坩埚：请腾出供应器回收栏空间。
    CrucibleReturnsFull("gtnr.gui.packaged.crucible_returns_full"),
    // #tr gtnr.gui.packaged.crucible_ready
    // # Crucible: alchemy complete; results are waiting to enter ME.
    // # zh_CN 坩埚：炼金术已完成，产物等待送入 ME。
    CrucibleReady("gtnr.gui.packaged.crucible_ready");

    public final String key;

    AltarStatus(String key) {
        this.key = key;
    }
}
