package com.xyp.gtneverreset.common.machine.multiMachineBase;

/**
 * 多方块控制器与公共机器基类的成员排布模板，顺序对照 GTNL 的 MachineBaseLayoutTemplate。
 * 本类仅供开发时参考，不承载机器逻辑，不注册，也不作为控制器的父类。
 *
 * <ol>
 * <li>静态常量与静态工具方法</li>
 * <li>持久化字段</li>
 * <li>运行时字段</li>
 * <li>构造器</li>
 * <li>生命周期方法</li>
 * <li>结构检查与参数准备方法</li>
 * <li>配方与处理主流程方法</li>
 * <li>输入、输出、仓室与贴图辅助方法</li>
 * <li>信息展示方法</li>
 * <li>UI 方法</li>
 * <li>NBT 与客户端同步方法</li>
 * <li>能力开关与策略 Getter</li>
 * <li>底层辅助方法与内部类型</li>
 * </ol>
 *
 * 同一职责的方法连续排列，覆写方法按职责归位；重载和流程专用辅助方法就近放置。
 * 未使用的区域可以省略。字段分区时必须检查初始化依赖，不能改变初始化行为。
 * 结构形状使用 MultiblockStructureLoader 加载 MBS1 资源，提示信息沿用自动生成与 Markdown 文档流程。
 * UI 实现在 common.gui.modularui，控制器只保留入口及必要的状态访问方法。
 */
public abstract class MachineBaseLayoutTemplate {
}
