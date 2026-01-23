package com.winter.common.model;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 玩家在内存中的“数字化分身”
 */
public class PlayerModel {
    // 基础属性
    private long playerId;
    private String name;
    private int level;

    // 资源属性 (对应数据库 player_main 表)
    private long wood;
    private long coal;
    private long food;

    // 位置坐标
    private float x;
    private float y;

    // 建筑列表 (对应数据库 player_building 表)
    // Key 为建筑类型 (1:熔炉, 2:兵营, 3:伐木场)，Value 为建筑对象
    private Map<Integer, BuildingModel> buildings = new ConcurrentHashMap<>();

    // 【重要】系统标记：标记该对象是否被修改过
    // 如果为 true，则定时任务会将其写入数据库
    private transient boolean isDirty = false;

    // 构造函数
    public PlayerModel(long playerId) {
        this.playerId = playerId;
    }

    // --- 业务逻辑方法 ---

    public void addWood(long amount) {
        this.wood += amount;
        this.isDirty = true; // 只要动了数据，就标记为“脏数据”
    }

    // --- Getters & Setters ---
    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public long getWood() { return wood; }
    public void setWood(long wood) { this.wood = wood; this.isDirty = true; }
    public long getCoal() { return coal; }
    public void setCoal(long coal) { this.coal = coal; this.isDirty = true; }
    public long getFood() { return food; }
    public void setFood(long food) { this.food = food; this.isDirty = true; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; this.isDirty = true; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.isDirty = true; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; this.isDirty = true; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; this.isDirty = true; }

    public boolean isDirty() { return isDirty; }
    public void setDirty(boolean dirty) { isDirty = dirty; }

    public Map<Integer, BuildingModel> getBuildings() { return buildings; }
}