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
        put("traps", "斜方肌");
        put("forearms", "前臂肌群");
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

        EQUIPMENT_DATABASE.put("高位下拉", new Equipment(
            "lat_pulldown",
            "高位下拉",
            "高位下拉主要训练背阔肌和上背部肌群，适合建立背部宽度和下拉发力模式。",
            "https://example.com/videos/lat_pulldown.mp4",
            Arrays.asList("back"),
            Arrays.asList("biceps", "shoulders"),
            "初级",
            "1. 胸部挺起，背部保持稳定\n2. 下拉至锁骨附近即可\n3. 不要用身体后仰甩动借力",
            Arrays.asList(
                new Exercise("ex_lat_pulldown_1", "标准高位下拉", "坐姿稳定躯干，将横杆下拉至上胸位置。",
                    Arrays.asList("调整大腿固定垫，使身体稳定", "双手略宽于肩握住横杆", "肩胛先下沉，再用背部带动手肘向下", "控制横杆缓慢回到起始位置"),
                    Arrays.asList("身体大幅后仰", "只用手臂拉动", "耸肩导致颈部紧张"),
                    "肩部或肘部不适时降低重量并缩小动作幅度",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_lat_pulldown_2", "窄握高位下拉", "窄握把下拉，更强调背阔肌下部和手肘向身体两侧夹紧。",
                    Arrays.asList("使用窄握把，身体保持直立", "手肘贴近身体向下拉", "最低点短暂停顿，感受背部收缩", "缓慢还原并保持肩胛控制"),
                    Arrays.asList("手腕过度弯折", "用肱二头肌主导发力"),
                    "先用轻重量找到背部发力，再逐步增加负荷",
                    Arrays.asList("进阶"), "中级")
            ),
            0
        ));

        EQUIPMENT_DATABASE.put("坐姿划船", new Equipment(
            "seated_row",
            "坐姿划船",
            "坐姿划船主要训练中背部、背阔肌和肩胛稳定能力，是背部厚度训练的基础动作。",
            "https://example.com/videos/seated_row.mp4",
            Arrays.asList("back"),
            Arrays.asList("biceps", "shoulders"),
            "初级",
            "1. 保持脊柱中立，不要弓背\n2. 先收肩胛再拉手柄\n3. 回放时控制速度",
            Arrays.asList(
                new Exercise("ex_seated_row_1", "标准坐姿划船", "坐姿拉动手柄至腹部附近，强调肩胛后缩。",
                    Arrays.asList("坐稳并让双脚踩实踏板", "背部挺直，双臂自然伸直", "肩胛向后收紧并拉手柄至腹部", "缓慢送回，保持背部张力"),
                    Arrays.asList("弓背拉动", "身体前后大幅摆动", "耸肩夹颈"),
                    "腰背不适者应减轻重量并保持躯干稳定",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_seated_row_2", "宽握坐姿划船", "宽握拉向下胸位置，更强调上背部和后三角参与。",
                    Arrays.asList("使用宽握把，手肘向身体两侧打开", "拉动时保持胸部挺起", "在末端夹紧肩胛", "控制手柄缓慢前送"),
                    Arrays.asList("手肘过度后拉", "肩膀前顶失控"),
                    "肩关节活动受限者先选择标准握距",
                    Arrays.asList("进阶"), "中级")
            ),
            0
        ));

        EQUIPMENT_DATABASE.put("助力引体", new Equipment(
            "assisted_pull_up",
            "助力引体",
            "助力引体通过配重辅助完成引体向上，主要训练背部和肱二头肌，适合作为引体进阶训练。",
            "https://example.com/videos/assisted_pull_up.mp4",
            Arrays.asList("back"),
            Arrays.asList("biceps", "shoulders"),
            "中级",
            "1. 辅助重量越大动作越轻松\n2. 保持核心收紧，避免身体晃动\n3. 下放时不要完全放松肩膀",
            Arrays.asList(
                new Exercise("ex_assisted_pull_up_1", "标准助力引体", "借助辅助踏板或跪垫完成完整引体向上。",
                    Arrays.asList("选择合适辅助重量并握住把手", "肩胛下沉，核心收紧", "用背部带动身体向上至下巴接近把手", "控制身体缓慢下降"),
                    Arrays.asList("下放过快", "身体摆动", "只用手臂硬拉"),
                    "肩部不稳定者不要在底部完全悬挂放松",
                    Arrays.asList("进阶"), "中级"),
                new Exercise("ex_assisted_pull_up_2", "慢速离心助力引体", "上拉后用较慢速度下降，强化背部控制。",
                    Arrays.asList("借助辅助重量拉至高点", "顶部保持背部收紧", "用3到5秒缓慢下降", "到底部前保持肩胛稳定"),
                    Arrays.asList("下降失控", "耸肩代偿", "辅助重量过轻导致动作变形"),
                    "离心训练强度较高，初次练习减少组数",
                    Arrays.asList("进阶"), "高级")
            ),
            0
        ));

        EQUIPMENT_DATABASE.put("蝴蝶机夹胸", new Equipment(
            "pec_deck",
            "蝴蝶机夹胸",
            "蝴蝶机夹胸主要孤立训练胸肌，动作轨迹稳定，适合新手学习胸部收缩。",
            "https://example.com/videos/pec_deck.mp4",
            Arrays.asList("chest"),
            Arrays.asList("shoulders"),
            "初级",
            "1. 肘部略低于肩或与肩同高\n2. 夹胸时不要耸肩\n3. 回放时控制拉伸幅度",
            Arrays.asList(
                new Exercise("ex_pec_deck_1", "标准蝴蝶机夹胸", "坐姿双臂向身体前方合拢，集中感受胸肌收缩。",
                    Arrays.asList("调整座椅高度，使把手与胸部同高", "背部贴紧靠垫，胸部挺起", "双臂向前合拢至胸前", "缓慢打开至胸肌有拉伸感"),
                    Arrays.asList("肩膀前顶", "手臂完全伸直锁死", "回放幅度过大"),
                    "肩前侧不适时减小打开幅度",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_pec_deck_2", "顶峰停顿夹胸", "在合拢位置短暂停顿，增强胸肌挤压感。",
                    Arrays.asList("按标准动作合拢把手", "在胸前位置停顿1到2秒", "保持胸部发力而非手臂用力", "控制速度回到起始位置"),
                    Arrays.asList("停顿时耸肩", "用惯性撞击把手"),
                    "使用比标准动作更轻的重量保证控制",
                    Arrays.asList("进阶"), "中级")
            ),
            0
        ));

        EQUIPMENT_DATABASE.put("卧推", new Equipment(
            "bench_press",
            "卧推",
            "卧推主要训练胸肌，同时需要肱三头肌和肩部协同发力，是上肢推举力量的核心动作。",
            "https://example.com/videos/bench_press.mp4",
            Arrays.asList("chest"),
            Arrays.asList("triceps", "shoulders"),
            "中级",
            "1. 肩胛后缩下沉，保持胸部稳定\n2. 杠铃下放至胸部中下方\n3. 使用大重量时需要保护",
            Arrays.asList(
                new Exercise("ex_bench_press_1", "标准卧推", "仰卧将杠铃从胸部推起，训练胸部推举力量。",
                    Arrays.asList("仰卧在卧推凳上，双脚踩实地面", "握距略宽于肩，肩胛后缩下沉", "控制杠铃下放至胸部中下方", "向上推起至手臂接近伸直"),
                    Arrays.asList("肩膀前顶", "臀部离开凳面", "杠铃路径过高压肩"),
                    "大重量训练必须使用保护架或同伴保护",
                    Arrays.asList("进阶"), "中级"),
                new Exercise("ex_bench_press_2", "暂停卧推", "杠铃触胸附近短暂停顿后再推起，提升控制和稳定性。",
                    Arrays.asList("按标准卧推动作下放杠铃", "在胸部附近停顿1秒", "保持肩胛稳定和全身张力", "平稳发力推起"),
                    Arrays.asList("停顿时放松身体", "反弹借力", "手腕后折"),
                    "暂停卧推强度较高，重量应低于常规卧推",
                    Arrays.asList("进阶"), "高级")
            ),
            0
        ));

        EQUIPMENT_DATABASE.put("哑铃飞鸟", new Equipment(
            "dumbbell_fly",
            "哑铃飞鸟",
            "哑铃飞鸟主要训练肩部三角肌中束，帮助提升肩部外展控制和肩宽视觉效果。",
            "https://example.com/videos/dumbbell_fly.mp4",
            Arrays.asList("shoulders"),
            Arrays.asList("traps"),
            "中级",
            "1. 手肘微屈，避免耸肩\n2. 哑铃抬至肩高附近即可\n3. 使用可控重量，不要甩动",
            Arrays.asList(
                new Exercise("ex_dumbbell_fly_1", "站姿哑铃侧平举", "双手持哑铃向身体两侧抬起，训练三角肌中束。",
                    Arrays.asList("双脚与髋同宽站立，核心收紧", "双手持哑铃自然垂于身体两侧", "手肘微屈向两侧抬至肩高", "控制哑铃缓慢下降"),
                    Arrays.asList("耸肩借力", "身体摆动甩起", "抬得过高导致肩部不适"),
                    "肩部有撞击感时降低高度或停止动作",
                    Arrays.asList("进阶"), "中级"),
                new Exercise("ex_dumbbell_fly_2", "坐姿哑铃侧平举", "坐姿减少身体摆动，更集中刺激肩部。",
                    Arrays.asList("坐在凳上，双脚踩实", "保持躯干稳定，手肘微屈", "向两侧平稳抬起哑铃", "缓慢下放并保持肩部张力"),
                    Arrays.asList("借助惯性", "手腕高于手肘过多"),
                    "选择较轻重量，优先保证动作轨迹稳定",
                    Arrays.asList("进阶"), "中级")
            ),
            0
        ));

        EQUIPMENT_DATABASE.put("肱二头肌弯举", new Equipment(
            "biceps_curl",
            "肱二头肌弯举",
            "肱二头肌弯举主要训练上臂前侧肱二头肌，可使用哑铃、杠铃或绳索完成。",
            "https://example.com/videos/biceps_curl.mp4",
            Arrays.asList("biceps"),
            Arrays.asList("forearms"),
            "初级",
            "1. 上臂贴近身体两侧\n2. 不要前后摆动借力\n3. 下放时保持控制",
            Arrays.asList(
                new Exercise("ex_biceps_curl_1", "标准肱二头肌弯举", "双手持重量屈肘上举，集中训练肱二头肌。",
                    Arrays.asList("站立或坐姿保持身体稳定", "上臂贴近身体两侧", "屈肘将重量弯举至胸前", "缓慢下放至手臂接近伸直"),
                    Arrays.asList("身体后仰甩起", "手肘前移过多", "下放过快"),
                    "肘部不适时减轻重量并缩小动作幅度",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_biceps_curl_2", "锤式弯举", "掌心相对进行弯举，兼顾肱二头肌和前臂参与。",
                    Arrays.asList("双手持哑铃，掌心相对", "保持上臂稳定", "屈肘将哑铃向上举起", "控制速度缓慢下放"),
                    Arrays.asList("肩膀前送", "手腕晃动", "借助身体摆动"),
                    "腕部不适时减少重量或改用绳索",
                    Arrays.asList("新手", "进阶"), "初级")
            ),
            0
        ));

        EQUIPMENT_DATABASE.put("肱三头下压", new Equipment(
            "triceps_pushdown",
            "肱三头下压",
            "肱三头下压主要训练上臂后侧肱三头肌，常使用绳索或直杆在龙门架上完成。",
            "https://example.com/videos/triceps_pushdown.mp4",
            Arrays.asList("triceps"),
            Arrays.asList("forearms"),
            "初级",
            "1. 上臂固定在身体两侧\n2. 下压时不要耸肩或身体前压\n3. 回放时控制速度，保持肱三头肌张力",
            Arrays.asList(
                new Exercise("ex_triceps_pushdown_1", "标准肱三头下压", "双手握住把手向下伸展手臂，集中训练肱三头肌。",
                    Arrays.asList("站在绳索滑轮前，双脚与髋同宽", "上臂贴近身体两侧，肘部固定", "向下压至手臂接近伸直", "缓慢回到起始位置，保持肘部稳定"),
                    Arrays.asList("肘部前后移动", "身体下压借力", "回放过快失去控制"),
                    "肘关节不适时降低重量，并避免完全锁死手肘",
                    Arrays.asList("新手", "进阶"), "初级"),
                new Exercise("ex_triceps_pushdown_2", "绳索肱三头下压", "使用绳索把手，在底部向两侧分开以增强肱三头肌收缩。",
                    Arrays.asList("双手握住绳索两端，身体微微前倾", "保持上臂稳定并向下压", "底部将绳索向身体两侧分开", "控制绳索缓慢回到起始位置"),
                    Arrays.asList("手腕过度弯折", "肩膀参与过多", "重量过大导致动作变形"),
                    "优先使用可控重量，避免用肩部和躯干代偿",
                    Arrays.asList("进阶"), "中级")
            ),
            0
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
            EQUIPMENT_DATABASE.get("腿弯曲"),
            EQUIPMENT_DATABASE.get("高位下拉"),
            EQUIPMENT_DATABASE.get("坐姿划船"),
            EQUIPMENT_DATABASE.get("助力引体"),
            EQUIPMENT_DATABASE.get("蝴蝶机夹胸"),
            EQUIPMENT_DATABASE.get("卧推"),
            EQUIPMENT_DATABASE.get("哑铃飞鸟"),
            EQUIPMENT_DATABASE.get("肱二头肌弯举"),
            EQUIPMENT_DATABASE.get("肱三头下压")
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
