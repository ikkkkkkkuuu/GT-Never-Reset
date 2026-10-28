package com.xyp.gtneverreset.utils.crafting.quantum.core.localization;

public enum PlayerMessages {

    CraftingNoPlan,
    CraftingNoNetOutput,
    CraftingTooLong;

    public String getTranslationKey() {
        switch (this) {
            case CraftingNoNetOutput:
                // #tr gtnr.quantum_crafting.message.CraftingNoNetOutput
                // # Patterns cannot yield a net output. Check cycles and missing external inputs.
                // # zh_CN 样板无法净产出请求物品，请检查循环样板及缺少的外部输入。
                return "gtnr.quantum_crafting.message.CraftingNoNetOutput";
            case CraftingTooLong:
                // #tr gtnr.quantum_crafting.message.CraftingTooLong
                // # Calculation timed out. Request fewer items or raise the server calculation limit.
                // # zh_CN 合成计算超时，请减少请求数量或调高服务器计算时限。
                return "gtnr.quantum_crafting.message.CraftingTooLong";
            default:
                // #tr gtnr.quantum_crafting.message.CraftingNoPlan
                // # Quantum crafting could not produce a plan.
                // # zh_CN 量子合成计算未能生成计划。
                return "gtnr.quantum_crafting.message.CraftingNoPlan";
        }
    }
}
