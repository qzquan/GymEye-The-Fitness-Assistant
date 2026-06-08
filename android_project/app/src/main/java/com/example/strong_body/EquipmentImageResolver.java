package com.example.strong_body;

import android.content.Context;

public final class EquipmentImageResolver {

    private EquipmentImageResolver() {
    }

    public static int getCoverResId(Context context, Equipment equipment) {
        return getDrawableResId(context, "equipment_" + equipment.getId() + "_cover");
    }

    public static int getAnatomyResId(Context context, Equipment equipment) {
        return getDrawableResId(context, "equipment_" + equipment.getId() + "_anatomy");
    }

    private static int getDrawableResId(Context context, String resourceName) {
        if (context == null || resourceName == null) return 0;
        return context.getResources().getIdentifier(
                resourceName,
                "drawable",
                context.getPackageName()
        );
    }
}
