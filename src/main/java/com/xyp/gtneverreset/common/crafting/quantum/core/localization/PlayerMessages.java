package com.xyp.gtneverreset.common.crafting.quantum.core.localization;

public enum PlayerMessages {

    CraftingNoPlan,
    CraftingNoNetOutput,
    CraftingTooLong,
    CraftingMissingIngredient,
    CraftingAmountOverflow,
    CraftingPlanUnbalanced,
    CraftingReplayLimit,
    CraftingRecipeStalled,
    CraftingShortageResolutionLimit;

    public String getTranslationKey() {
        switch (this) {
            case CraftingNoNetOutput:
                // #tr gtnr.quantum_crafting.message.crafting_no_net_output
                // # Patterns cannot yield a net output. Check cycles and missing external inputs.
                // # zh_CN 样板无法净产出请求物品，请检查循环样板及缺少的外部输入。
                return "gtnr.quantum_crafting.message.crafting_no_net_output";
            case CraftingTooLong:
                // #tr gtnr.quantum_crafting.message.crafting_too_long
                // # Calculation timed out. Request fewer items or raise the server calculation limit.
                // # zh_CN 合成计算超时，请减少请求数量或调高服务器计算时限。
                return "gtnr.quantum_crafting.message.crafting_too_long";
            case CraftingMissingIngredient:
                // #tr gtnr.quantum_crafting.message.crafting_missing_ingredient
                // # An ingredient of the quantum crafting plan could not be crafted or found.
                // # zh_CN 量子合成计划中有原料既无法合成也无法找到。
                return "gtnr.quantum_crafting.message.crafting_missing_ingredient";
            case CraftingAmountOverflow:
                // #tr gtnr.quantum_crafting.message.crafting_amount_overflow
                // # Item count exceeds 9,223,372,036,854,775,807. Request a smaller amount.
                // # zh_CN 量子合成计划所需某物品的数量超出了可计数的上限（9,223,372,036,854,775,807）。请减少请求数量。
                return "gtnr.quantum_crafting.message.crafting_amount_overflow";
            case CraftingPlanUnbalanced:
                // #tr gtnr.quantum_crafting.message.crafting_plan_unbalanced
                // # The quantum crafting plan does not balance: its patterns cannot make what they consume.
                // # zh_CN 量子合成计划无法平衡：样板产出不足以抵消其消耗。
                return "gtnr.quantum_crafting.message.crafting_plan_unbalanced";
            case CraftingReplayLimit:
                // #tr gtnr.quantum_crafting.message.crafting_replay_limit
                // # Checking the quantum crafting plan took too many crafting passes. Request a smaller amount.
                // # zh_CN 校验量子合成计划所需的合成轮数过多。请减少请求数量。
                return "gtnr.quantum_crafting.message.crafting_replay_limit";
            case CraftingRecipeStalled:
                // #tr gtnr.quantum_crafting.message.crafting_recipe_stalled
                // # The quantum crafting plan would leave a crafting step waiting for inputs that never arrive.
                // # zh_CN 量子合成计划会让某个合成步骤一直等待永远不会到达的原料。
                return "gtnr.quantum_crafting.message.crafting_recipe_stalled";
            case CraftingShortageResolutionLimit:
                // #tr gtnr.quantum_crafting.message.crafting_shortage_resolution_limit
                // # Could not resolve missing crafting ingredients. Request a smaller amount.
                // # zh_CN 量子合成计算无法确定计划缺少的物品。请减少请求数量。
                return "gtnr.quantum_crafting.message.crafting_shortage_resolution_limit";
            default:
                // #tr gtnr.quantum_crafting.message.crafting_no_plan
                // # Quantum crafting could not produce a plan.
                // # zh_CN 量子合成计算未能生成计划。
                return "gtnr.quantum_crafting.message.crafting_no_plan";
        }
    }
}
