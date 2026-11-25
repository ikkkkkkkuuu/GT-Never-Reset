package com.xyp.gtneverreset.compat.ae2conveniencefeatures;

import java.lang.reflect.Method;

import com.xyp.gtnrlib.mod.ModList;

import appeng.client.me.ItemRepo;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class AE2ConvenienceCompletion {

    private static final ClassValue<Access> ACCESS = new ClassValue<>() {

        @Override
        protected Access computeValue(Class<?> type) {
            try {
                return new Access(type.getMethod("ae2cf$prefixSlots"), type.getMethod("ae2cf$refresh"));
            } catch (NoSuchMethodException ignored) {
                return new Access(null, null);
            }
        }
    };

    private AE2ConvenienceCompletion() {}

    public static int prefixSlots(ItemRepo repo) {
        if (repo == null || !ModList.AE2ConvenienceFeatures.isModLoaded()) return 0;
        Method prefix = ACCESS.get(repo.getClass())
            .prefix();
        if (prefix == null) return 0;
        try {
            return prefix.invoke(repo) instanceof Integer slots ? slots : 0;
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    public static void reflow(ItemRepo repo) {
        if (!ModList.AE2ConvenienceFeatures.isModLoaded()) return;
        Method refresh = ACCESS.get(repo.getClass())
            .refresh();
        if (refresh == null) return;
        try {
            refresh.invoke(repo);
        } catch (ReflectiveOperationException ignored) {}
    }

    private record Access(Method prefix, Method refresh) {}
}
