export const TRAINING_EQUIPMENT = [
  {
    id: 'leg_press',
    name: '倒蹬机',
    actionName: '标准倒蹬',
    targetMuscles: ['quadriceps', 'glutes'],
    groups: ['legs', 'strength']
  },
  {
    id: 'shoulder_press',
    name: '坐姿推肩',
    actionName: '坐姿推肩',
    targetMuscles: ['shoulders', 'triceps'],
    groups: ['shoulders', 'push']
  },
  {
    id: 'leg_extension',
    name: '腿屈伸',
    actionName: '腿屈伸',
    targetMuscles: ['quadriceps'],
    groups: ['legs', 'rehab']
  },
  {
    id: 'leg_curl',
    name: '腿弯曲',
    actionName: '腿弯曲',
    targetMuscles: ['hamstrings', 'glutes'],
    groups: ['legs', 'rehab']
  },
  {
    id: 'lat_pulldown',
    name: '高位下拉',
    actionName: '高位下拉',
    targetMuscles: ['back', 'biceps'],
    groups: ['back', 'pull']
  },
  {
    id: 'seated_row',
    name: '坐姿划船',
    actionName: '坐姿划船',
    targetMuscles: ['back', 'biceps'],
    groups: ['back', 'pull', 'rehab']
  },
  {
    id: 'assisted_pull_up',
    name: '助力引体',
    actionName: '助力引体向上',
    targetMuscles: ['back', 'biceps'],
    groups: ['back', 'pull', 'strength']
  },
  {
    id: 'pec_deck',
    name: '蝴蝶机夹胸',
    actionName: '蝴蝶机夹胸',
    targetMuscles: ['chest', 'shoulders'],
    groups: ['chest', 'push']
  },
  {
    id: 'bench_press',
    name: '卧推',
    actionName: '卧推',
    targetMuscles: ['chest', 'triceps', 'shoulders'],
    groups: ['chest', 'push', 'strength']
  },
  {
    id: 'dumbbell_fly',
    name: '哑铃飞鸟',
    actionName: '哑铃侧平举',
    targetMuscles: ['shoulders', 'traps'],
    groups: ['shoulders', 'shape']
  },
  {
    id: 'biceps_curl',
    name: '肱二头肌弯举',
    actionName: '肱二头肌弯举',
    targetMuscles: ['biceps', 'forearms'],
    groups: ['arms', 'pull']
  },
  {
    id: 'triceps_pushdown',
    name: '肱三头下压',
    actionName: '绳索下压',
    targetMuscles: ['triceps', 'forearms'],
    groups: ['arms', 'push']
  },
  {
    id: 'treadmill',
    name: '跑步机',
    actionName: '跑步机有氧',
    targetMuscles: ['cardio'],
    groups: ['cardio', 'fat_loss'],
    durationOnly: true
  }
];

export const TRAINING_EQUIPMENT_BY_ID = new Map(TRAINING_EQUIPMENT.map(item => [item.id, item]));

