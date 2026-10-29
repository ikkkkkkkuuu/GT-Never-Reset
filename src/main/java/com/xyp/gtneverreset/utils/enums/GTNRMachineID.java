package com.xyp.gtneverreset.utils.enums;

public enum GTNRMachineID {

    // region 多方块
    // ID 范围：29400–29499；新增条目从本分区预留范围依次分配，不占用其他分区。
    LargeOreProcessor(29400),
    LargeFurnace(29401),
    QuantumComputer(29402),
    AssemblerMatrix(29403),
    SingularityDataHub(29404),
    LargeBeeBreeder(29405),
    // 预留：29406–29499（94 个）。
    // endregion

    // region 舱室
    // ID 范围：29500–29599；新增条目从本分区预留范围依次分配，不占用其他分区。
    SuperCraftingInputME(29500),
    CircuitMEPatternBuffer(29501),
    SuperCraftingInputSlave(29502),
    CircuitMEPatternMirror(29503),
    MaxCapacityMEOutputBus(29504),
    MaxCapacityMEOutputHatch(29505),
    MEOutputAssembly(29506),
    VaultPortHatch(29507),

    StaticParallelControllerT1(29508),
    StaticParallelControllerT2(29509),
    StaticParallelControllerT3(29510),
    StaticParallelControllerT4(29511),
    StaticParallelControllerT5(29512),
    StaticParallelControllerT6(29513),
    StaticParallelControllerT7(29514),
    StaticParallelControllerT8(29515),

    DynamicParallelControllerT1(29516),
    DynamicParallelControllerT2(29517),
    DynamicParallelControllerT3(29518),
    DynamicParallelControllerT4(29519),
    DynamicParallelControllerT5(29520),
    DynamicParallelControllerT6(29521),
    DynamicParallelControllerT7(29522),
    DynamicParallelControllerT8(29523),

    StaticSpeedControllerT1(29524),
    StaticSpeedControllerT2(29525),
    StaticSpeedControllerT3(29526),
    StaticSpeedControllerT4(29527),
    StaticSpeedControllerT5(29528),
    StaticSpeedControllerT6(29529),
    StaticSpeedControllerT7(29530),
    StaticSpeedControllerT8(29531),

    DynamicSpeedControllerT1(29532),
    DynamicSpeedControllerT2(29533),
    DynamicSpeedControllerT3(29534),
    DynamicSpeedControllerT4(29535),
    DynamicSpeedControllerT5(29536),
    DynamicSpeedControllerT6(29537),
    DynamicSpeedControllerT7(29538),
    DynamicSpeedControllerT8(29539),

    PowerConsumptionControllerT1(29540),
    PowerConsumptionControllerT2(29541),
    PowerConsumptionControllerT3(29542),
    PowerConsumptionControllerT4(29543),
    PowerConsumptionControllerT5(29544),
    PowerConsumptionControllerT6(29545),
    PowerConsumptionControllerT7(29546),
    PowerConsumptionControllerT8(29547),

    LowSpeedPerfectOverclockController(29548),
    PerfectOverclockController(29549),
    SingularityPerfectOverclockController(29550),
    SuperAdvancedMEInputBus(29551),
    SuperAdvancedMEInputHatch(29552),
    MEDataAccessHatch(29553),
    // 预留：29554–29599（46 个）。
    // endregion

    // region 单方块机器
    // ID 范围：29600–29899；新增条目从本分区预留范围依次分配，不占用其他分区。
    FragmentWorldCollector(29600),

    EndCrystalResonatorLV(29601),
    EndCrystalResonatorMV(29602),
    EndCrystalResonatorHV(29603),
    EndCrystalResonatorEV(29604),
    EndCrystalResonatorIV(29605),
    EndCrystalResonatorLuV(29606),
    EndCrystalResonatorZPM(29607),
    EndCrystalResonatorUV(29608),
    EndCrystalResonatorUHV(29609),
    EndCrystalResonatorUEV(29610),
    EndCrystalResonatorUIV(29611),
    EndCrystalResonatorUMV(29612),
    EndCrystalResonatorUXV(29613),
    EndCrystalResonatorMAX(29614),

    ModernMixerLV(29615),
    ModernMixerMV(29616),
    ModernMixerHV(29617),
    ModernMixerEV(29618),
    ModernMixerIV(29619),
    ModernMixerLuV(29620),
    ModernMixerZPM(29621),
    ModernMixerUV(29622),

    ModernElectrolyzerLV(29623),
    ModernElectrolyzerMV(29624),
    ModernElectrolyzerHV(29625),
    ModernElectrolyzerEV(29626),
    ModernElectrolyzerIV(29627),
    ModernElectrolyzerLuV(29628),
    ModernElectrolyzerZPM(29629),
    ModernElectrolyzerUV(29630),

    ModernChemicalReactorLV(29631),
    ModernChemicalReactorMV(29632),
    ModernChemicalReactorHV(29633),
    ModernChemicalReactorEV(29634),
    ModernChemicalReactorIV(29635),
    ModernChemicalReactorLuV(29636),
    ModernChemicalReactorZPM(29637),
    ModernChemicalReactorUV(29638),

    ModernCentrifugeLV(29639),
    ModernCentrifugeMV(29640),
    ModernCentrifugeHV(29641),
    ModernCentrifugeEV(29642),
    ModernCentrifugeIV(29643),
    ModernCentrifugeLuV(29644),
    ModernCentrifugeZPM(29645),
    ModernCentrifugeUV(29646);
    // 预留：29647–29899（253 个）。
    // endregion

    public final int id;

    GTNRMachineID(int id) {
        this.id = id;
    }
}
