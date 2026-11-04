package com.xyp.gtnrlib.compat.draconicevolution;

import net.minecraft.client.model.ModelBiped;

import com.brandon3055.draconicevolution.client.model.ModelDraconicArmor;
import com.brandon3055.draconicevolution.client.model.ModelWyvernArmor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class DraconicArmorArms {

    public static float scale(ModelBiped model, float vanillaScale) {
        // These OBJ arms cache the scale from their first draw, including when first seen in an inventory.
        return model instanceof ModelDraconicArmor || model instanceof ModelWyvernArmor ? 1f / 15 : vanillaScale;
    }

    private DraconicArmorArms() {}
}
