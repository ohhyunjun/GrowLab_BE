package com.metaverse.growlab_be.harvest;

import com.metaverse.growlab_be.species.domain.Species;
import java.util.Locale;

public final class HarvestCrop {
    private HarvestCrop() {}
    public static boolean supports(Species species) {
        if (species == null || species.getName() == null) return false;
        String name = species.getName().toLowerCase(Locale.ROOT);
        return name.contains("상추") || name.contains("lettuce");
    }
}
