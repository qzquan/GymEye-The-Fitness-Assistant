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
        // 倒蹬机（后端暂无对应记录，backendId=0）
        EQUIPMENT_DATABASE.put("倒蹬机", new Equipment(
            "leg_press",
            "倒蹬机",
            "倒蹬机是锻炼下肢力量的经典器械，主要针对大腿前侧股四头肌和臀部肌群。",
            "https://example.com/videos/leg_press.mp4",
            Arrays.asList("quadriceps", "glutes"),
            Arrays.asList("hamstrings", "calves"),
            "中级",
            "1. 脚距与肩同宽\n2. 下放时膝盖不超过脚尖\n3. 不要完全锁死关节",
            Arrays.asList(
                new Exercise("ex_leg_press_1", "标准倒蹬", "双脚与肩同宽，匀速推放。",
                    Arrays.asList("调整座椅靠背，使膝盖与脚尖方向一致", "双脚踏在踏板上，与肩同宽", "缓慢下放至膝盖约90度", "匀速推起，不要锁死膝盖"),
                    Arrays.asList("膝盖内扣", "腰部离开靠垫", "推放速度过快"),
                    "新手建议在教练指导下使用，重量循序渐进",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_leg_press_2", "窄距倒蹬", "双脚窄于肩宽，更侧重股四头肌外侧。",
                    Arrays.asList("双脚并拢或窄于肩宽踏在踏板上", "下放时保持膝盖与脚尖方向一致", "推起时感受大腿外侧发力"),
                    Arrays.asList("脚距过窄导致膝盖压力过大", "下放幅度过大"),
                    "膝盖有不适时立即停止",
                    Arrays.asList("进阶"), "中级"),
                new Exercise("ex_leg_press_3", "宽距倒蹬", "双脚宽于肩宽，更侧重臀部和内收肌。",
                    Arrays.asList("双脚宽于肩宽，脚尖略向外", "下放时膝盖跟随脚尖方向打开", "推起时感受臀部和大腿内侧发力"),
                    Arrays.asList("脚尖方向与膝盖不一致", "骨盆晃动"),
                    "柔韧性不足者不要勉强大幅下放",
                    Arrays.asList("进阶"), "中级")
            ),
            0  // 后端暂无对应记录
        ));

        // 坐姿推肩（后端 equipment id=1）
        EQUIPMENT_DATABASE.put("坐姿推肩", new Equipment(
            "shoulder_press",
            "坐姿推肩",
            "坐姿推肩器主要锻炼肩部三角肌，同时也会用到上胸肌和肱三头肌。",
            "https://example.com/videos/shoulder_press.mp4",
            Arrays.asList("shoulders"),
            Arrays.asList("triceps", "chest"),
            "初级",
            "1. 背部紧贴靠垫\n2. 推举时手臂不要完全伸直\n3. 控制速度，避免晃动",
            Arrays.asList(
                new Exercise("ex_shoulder_press_1", "标准推肩", "背部贴紧靠垫，匀速推举。",
                    Arrays.asList("调整座椅高度，使手柄与肩同高", "背部紧贴靠垫，核心收紧", "向上推举至手臂接近伸直", "缓慢下放至起始位置"),
                    Arrays.asList("腰部过度前凸", "耸肩借力", "推举时身体晃动"),
                    "肩部有伤痛者请先咨询医生",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_shoulder_press_2", "单臂推肩", "单侧交替推举，改善左右力量不平衡。",
                    Arrays.asList("单手握住手柄，另一手扶住座椅", "单侧匀速推举", "两侧交替进行，次数相同"),
                    Arrays.asList("身体向一侧倾斜", "借助惯性甩起"),
                    "建议先掌握标准推肩后再尝试单侧训练",
                    Arrays.asList("进阶"), "中级")
            ),
            1  // 后端 equipment id=1
        ));

        // 腿屈伸（后端 equipment id=2）
        EQUIPMENT_DATABASE.put("腿屈伸", new Equipment(
            "leg_extension",
            "腿屈伸",
            "腿屈伸器械专门针对股四头肌，是孤立训练大腿前侧的最佳选择。",
            "https://www.cdc.gov/physicalactivity/videos/Leg_extension_Ipod-Lg.mp4",
            Arrays.asList("quadriceps"),
            Arrays.asList("abs"),
            "初级",
            "1. 调节垫子位置贴合脚踝\n2. 举起时不要锁死膝盖\n3. 速度要慢，感受肌肉发力",
            Arrays.asList(
                new Exercise("ex_leg_ext_1", "标准腿屈伸", "坐姿匀速伸展膝关节。",
                    Arrays.asList("调整靠垫贴合小腿前侧", "双手握住扶手，背部贴紧", "缓慢伸直小腿至最高点", "控制速度缓慢下放"),
                    Arrays.asList("甩腿借力", "膝盖完全锁死", "臀部离开坐垫"),
                    "下放时不要完全放松，保持肌肉张力",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_leg_ext_2", "单腿屈伸", "单侧训练，纠正左右腿力量差异。",
                    Arrays.asList("单侧小腿贴合靠垫", "单腿匀速伸展", "两侧交替进行"),
                    Arrays.asList("身体扭转借力", "速度过快"),
                    "重量应比双腿时适当降低",
                    Arrays.asList("进阶", "康复"), "初级")
            ),
            2  // 后端 equipment id=2
        ));

        // 腿弯曲（后端 equipment id=3）
        EQUIPMENT_DATABASE.put("腿弯曲", new Equipment(
            "leg_curl",
            "腿弯曲",
            "腿弯举器械主要用于锻炼大腿后侧的腘绳肌群。",
            "https://example.com/videos/leg_curl.mp4",
            Arrays.asList("hamstrings"),
            Arrays.asList("glutes"),
            "初级",
            "1. 垫子位置要在脚踝上方\n2. 弯举时臀部不要离开坐垫\n3. 下降时要控制速度",
            Arrays.asList(
                new Exercise("ex_leg_curl_1", "标准腿弯举", "俯卧匀速弯曲膝关节。",
                    Arrays.asList("俯卧在器械上，垫子贴合脚踝后侧", "双手握住扶手", "缓慢弯曲小腿至臀部方向", "控制速度缓慢伸直"),
                    Arrays.asList("臀部抬起借力", "速度过快甩腿", "腰部过度反弓"),
                    "腘绳肌较紧的人注意不要强行弯曲",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_leg_curl_2", "单腿弯举", "单侧训练，强化腘绳肌单独发力能力。",
                    Arrays.asList("单侧脚踝贴合靠垫", "单腿匀速弯曲", "两侧交替完成相同次数"),
                    Arrays.asList("骨盆歪斜", "借助惯性"),
                    "重量应适当降低，注重动作质量",
                    Arrays.asList("进阶"), "中级")
            ),
            3  // 后端 equipment id=3
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
