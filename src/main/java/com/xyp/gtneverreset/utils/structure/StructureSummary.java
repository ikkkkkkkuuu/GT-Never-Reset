// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GTNL PR #654, commit 1cd98b4f61e4c77f65263171d3f03dda9c813382.
// Structure variants updated from commit a6ee80aea6eaf98e2478e847a96dfdc3a620847b.
package com.xyp.gtneverreset.utils.structure;

import java.util.List;

import gregtech.api.structure.IStructureChannels;

public record StructureSummary(Size size, boolean hollow, ControllerPosition controller, List<BlockLine> blocks,
    List<HatchLine> hatches, List<IStructureChannels> channels, Size allTiers, List<Section> sections,
    boolean layered) {

    public StructureSummary(Size size, boolean hollow, ControllerPosition controller, List<BlockLine> blocks,
        List<HatchLine> hatches, List<IStructureChannels> channels) {
        this(
            size,
            hollow,
            controller,
            blocks,
            hatches,
            channels,
            size,
            List.of(new Section(blocks, List.of(), null, List.of())),
            false);
    }

    public boolean hasIndependentTiers() {
        return sections.stream()
            .anyMatch(section -> section.details() != null);
    }

    public boolean hasMaterialTiers() {
        return sections.stream()
            .anyMatch(
                section -> !section.materialTiers()
                    .isEmpty());
    }

    public record Size(int width, int height, int depth) {}

    public record ControllerPosition(int layer, boolean frontCenter) {}

    public record BlockLine(String count, List<String> names, boolean tiered) {

        public BlockLine(String count, List<String> names) {
            this(count, names, false);
        }
    }

    public record HatchLine(String count, List<String> nameKeys, List<String> replaces, List<String> locations) {}

    public record MaterialTier(String header, List<BlockLine> blocks) {}

    public record TierDetails(Size size, boolean hollow, ControllerPosition controller, List<HatchLine> hatches) {}

    public record Section(List<BlockLine> blocks, List<MaterialTier> materialTiers, TierDetails details,
        List<String> casingRequirements, List<HatchLine> hatches) {

        public Section(List<BlockLine> blocks, List<MaterialTier> materialTiers, TierDetails details,
            List<String> casingRequirements) {
            this(blocks, materialTiers, details, casingRequirements, List.of());
        }
    }
}
