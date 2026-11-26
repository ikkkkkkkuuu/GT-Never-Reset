package com.xyp.gtneverreset.common.projector;

import java.util.Collections;
import java.util.List;

import com.xyp.gtneverreset.common.projector.ProjectorStructure.Material;

final class ProjectorMaterialPlan {

    private ProjectorMaterialPlan() {}

    static List<Material> materials(ProjectorStructure shape, ProjectorPlan plan) {
        if (shape == null || shape.isEmpty()
            || !shape.settings.equals(plan.settings())
            || !shape.failure.isEmpty()
            || !shape.limit.isEmpty()) return Collections.emptyList();
        if (!plan.batchProjection || plan.batch.machines() == 1) return shape.materials;
        if (!ProjectorBatchCells.fitsPreview(shape, plan.batch)) return Collections.emptyList();

        var layout = new ProjectorBatch.Layout(plan.batch, shape, plan.facing);
        if (!layout.solve()) return Collections.emptyList();

        var batch = ProjectorBatchCells.collect(shape, plan, plan.facing, layout, null);
        return batch.compatible() ? batch.materials(shape) : Collections.emptyList();
    }
}
