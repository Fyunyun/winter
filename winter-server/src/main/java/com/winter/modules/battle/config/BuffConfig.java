package com.winter.modules.battle.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Buff 配置表条目 —— 描述一个 Buff 类型的全部元数据
 *
 * 从 buff_config.json 加载，运行时由 {@link BuffConfigTable} 缓存。
 *
 * 字段说明：
 * - buffId:       唯一标识
 * - name:         显示名称
 * - category:     行为分类（对应 {@link BuffCategory}）
 * - attrType:     影响的属性（ATK / DEF / SPEED 等，仅 ATTR_MOD 类使用）
 * - positive:     是增益(true)还是减益(false)
 * - tickTiming:   DOT/HOT 结算时机（ROUND_START / ROUND_END）
 * - stackable:    是否可叠加
 * - maxStack:     最大叠加层数
 * - defaultValue: 默认数值（可被技能参数覆盖）
 * - tags:         标签列表（用于筛选/驱散/免疫等）
 */
@Getter
@Setter
public class BuffConfig {
    private int buffId;
    private String name;
    private String category;      // 字符串，对应 BuffCategory
    private String attrType;      // ATK, DEF, SPEED 等
    private boolean positive;     // true=增益, false=减益
    private String tickTiming;    // ROUND_START / ROUND_END
    private boolean stackable;
    private int maxStack;
    private int defaultValue;
    private List<String> tags;

    /**
     * 获取行为分类枚举
     */
    public BuffCategory getBuffCategory() {
        return BuffCategory.fromCode(category);
    }

    /**
     * 是否是 DOT 类（持续伤害）
     */
    public boolean isDot() {
        return BuffCategory.DOT == getBuffCategory();
    }

    /**
     * 是否是护盾类
     */
    public boolean isShield() {
        return BuffCategory.SHIELD == getBuffCategory();
    }

    /**
     * 是否是控制类
     */
    public boolean isControl() {
        return BuffCategory.CONTROL == getBuffCategory();
    }

    /**
     * 是否是属性修改类
     */
    public boolean isAttrMod() {
        return BuffCategory.ATTR_MOD == getBuffCategory();
    }

    /**
     * 结算时机是否匹配
     */
    public boolean matchTiming(String timing) {
        return timing != null && timing.equalsIgnoreCase(this.tickTiming);
    }
}
