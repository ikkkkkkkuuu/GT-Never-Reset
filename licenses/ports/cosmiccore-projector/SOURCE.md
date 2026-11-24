# CosmicCore deployment animation

- Upstream source: Frontiers-PackForge/CosmicCore, commit `19f893ee163806d62ad6beeafb80056120d07a34`
- File: `src/main/java/com/ghostipedia/cosmiccore/common/deployment/LeylineDeploymentAnimation.java`
- Source: https://github.com/Frontiers-PackForge/CosmicCore/blob/19f893ee163806d62ad6beeafb80056120d07a34/src/main/java/com/ghostipedia/cosmiccore/common/deployment/LeylineDeploymentAnimation.java
- License: LGPL-3.0 under the upstream README. The accompanying LGPL and GPL texts apply to the adapted code.
- GTNR adaptation (2026-11-23): `gtnrlib/src/main/java/com/xyp/gtnrlib/animation/StructureDeployment.java` reuses the timing and easing calculations, with 1.7.10 math helpers and bounds captured by GTNR's build preflight. The circle geometry is authored in GTNR.
- Impact source: https://github.com/Frontiers-PackForge/CosmicCore/blob/19f893ee163806d62ad6beeafb80056120d07a34/src/main/java/com/ghostipedia/cosmiccore/client/renderer/deployment/LeylineDeploymentEffects.java
- GTNR impact adaptation (2026-11-23): `gtnrlib/src/main/java/com/xyp/gtnrlib/client/fx/world/FxStructureImpact.java` adapts the three dust waves, rectangular footprint sampling and debris velocities to 1.7.10 particles. The camera shake is authored in GTNR.
