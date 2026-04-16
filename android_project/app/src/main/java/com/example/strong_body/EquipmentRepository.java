package com.example.strong_body;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健身器材数据仓库
 * 包含所有器材的教学视频URL和肌肉群信息
 */
public class EquipmentRepository {

    // 肌肉群中英文对照表
    public static final Map<String, String> MUSCLE_NAMES_CN = new HashMap<String, String>() {{
        put("quadriceps", "股四头肌");
        put("hamstrings", "腘绳肌");
        put("glutes", "臀大肌");
        put("calves", "小腿肌群");
        put("shoulders", "三角肌");
        put("chest", "胸肌");
        put("triceps", "肱三头肌");
        put("biceps", "肱二头肌");
        put("back", "背部肌群");
        put("abs", "腹肌");
    }};

    // 器材数据库
    private static final Map<String, Equipment> EQUIPMENT_DATABASE = new HashMap<>();

    static {
        // 倒蹬机
        EQUIPMENT_DATABASE.put("倒蹬机", new Equipment(
            "leg_press",
            "倒蹬机",
            "倒蹬机是锻炼下肢力量的经典器械，主要针对大腿前侧股四头肌和臀部肌群。",
            "https://example.com/videos/leg_press.mp4",
            Arrays.asList("quadriceps", "glutes"),
            Arrays.asList("hamstrings", "calves"),
            "中级",
            "1. 脚距与肩同宽\n2. 下放时膝盖不超过脚尖\n3. 不要完全锁死关节"
        ));

        // 坐姿推肩
        EQUIPMENT_DATABASE.put("坐姿推肩", new Equipment(
            "shoulder_press",
            "坐姿推肩",
            "坐姿推肩器主要锻炼肩部三角肌，同时也会用到上胸肌和肱三头肌。",
            "https://example.com/videos/shoulder_press.mp4",
            Arrays.asList("shoulders"),
            Arrays.asList("triceps", "chest"),
            "初级",
            "1. 背部紧贴靠垫\n2. 推举时手臂不要完全伸直\n3. 控制速度，避免晃动"
        ));

        // 腿屈伸
        EQUIPMENT_DATABASE.put("腿屈伸", new Equipment(
            "leg_extension",
            "腿屈伸",
            "腿屈伸器械专门针对股四头肌，是孤立训练大腿前侧的最佳选择。",
            "https://example.com/videos/leg_extension.mp4",
            Arrays.asList("quadriceps"),
            Arrays.asList("abs"),
            "初级",
            "1. 调节垫子位置贴合脚踝\n2. 举起时不要锁死膝盖\n3. 速度要慢，感受肌肉发力"
        ));

        // 腿弯曲
        EQUIPMENT_DATABASE.put("腿弯曲", new Equipment(
            "leg_curl",
            "腿弯曲",
            "腿弯举器械主要用于锻炼大腿后侧的腘绳肌群。",
            "https://example.com/videos/leg_curl.mp4",
            Arrays.asList("hamstrings"),
            Arrays.asList("glutes"),
            "初级",
            "1. 垫子位置要在脚踝上方\n2. 弯举时臀部不要离开坐垫\n3. 下降时要控制速度"
        ));
    }

    /**
     * 根据器材名称获取器材信息
     */
    public static Equipment getEquipmentByName(String name) {
        // 移除置信度百分比部分
        String cleanName = name.replaceAll("\\(\\d+%\\)", "").trim();
        return EQUIPMENT_DATABASE.get(cleanName);
    }

    /**
     * 根据器材ID获取器材信息
     */
    public static Equipment getEquipmentById(String id) {
        for (Equipment equipment : EQUIPMENT_DATABASE.values()) {
            if (equipment.getId().equals(id)) {
                return equipment;
            }
        }
        return null;
    }

    /**
     * 获取所有器材列表
     */
    public static List<Equipment> getAllEquipment() {
        return Arrays.asList(
            EQUIPMENT_DATABASE.get("倒蹬机"),
            EQUIPMENT_DATABASE.get("坐姿推肩"),
            EQUIPMENT_DATABASE.get("腿屈伸"),
            EQUIPMENT_DATABASE.get("腿弯曲")
        );
    }

    /**
     * 获取肌肉群中文名称
     */
    public static String getMuscleNameCn(String muscleEn) {
        return MUSCLE_NAMES_CN.getOrDefault(muscleEn, muscleEn);
    }

    /**
     * 获取肌肉颜色（用于肌肉图高亮）
     * 主要肌肉群 - 红色系
     * 次要肌肉群 - 橙色系
     */
    public static int getMuscleHighlightColor(boolean isPrimary) {
        // 主要肌肉群使用较深的红色
        // 次要肌肉群使用较浅的橙色
        return isPrimary ? 0xFFFF4444 : 0xFFFF8800;
    }
}
