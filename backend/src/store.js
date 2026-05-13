import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const dataDir = path.resolve(__dirname, '../data');
const dataPath = path.join(dataDir, 'db.json');
const tempDataPath = path.join(dataDir, 'db.json.tmp');
const labelCandidates = [
  path.resolve(__dirname, '../../android_project/app/src/main/assets/labels.txt'),
  path.resolve(__dirname, '../../android_project/app/src/main/assets/label.txt')
];

const defaultState = {
  users: [],
  equipment: [],
  history: [],
  body_parts: [],
  difficulty_levels: [],
  exercises: [],
  exercise_videos: [],
  counters: {
    users: 0,
    equipment: 0,
    history: 0,
    body_parts: 0,
    difficulty_levels: 0,
    exercises: 0,
    exercise_videos: 0
  }
};

let readyPromise = null;
let mutationQueue = Promise.resolve();

async function ensureReady() {
  if (!readyPromise) {
    readyPromise = initialize();
  }
  await readyPromise;
}

async function initialize() {
  await fs.mkdir(dataDir, { recursive: true });
  let dataFileExists = true;
  try {
    await fs.access(dataPath);
  } catch {
    dataFileExists = false;
  }

  if (dataFileExists) {
    const state = await readStateFile();
    const normalized = await normalizeState(state);
    if (normalized.changed) {
      await writeState(normalized.state);
    }
    return;
  }

  const initialState = structuredClone(defaultState);
  initialState.equipment = await loadLabelEquipment();
  initialState.counters.equipment = initialState.equipment.length;
  const normalized = await normalizeState(initialState);
  await writeState(normalized.state);
}

async function loadLabelEquipment() {
  for (const labelsPath of labelCandidates) {
    try {
      const content = await fs.readFile(labelsPath, 'utf-8');
      return content
        .split(/\r?\n/)
        .map(line => line.trim())
        .filter(Boolean)
        .map((name, index) => ({
          id: index + 1,
          name,
          description: null,
          target_muscle: null,
          tutorial_text: null,
          tutorial_url: null,
          category: null,
          suitable_for: null,
          created_at: new Date().toISOString()
        }));
    } catch {}
  }
  return [];
}

function maxId(rows) {
  return rows.reduce((max, row) => (Number.isInteger(row.id) && row.id > max ? row.id : max), 0);
}

async function normalizeState(state) {
  let changed = false;
  const normalized = {
    ...structuredClone(defaultState),
    ...state,
    counters: {
      ...structuredClone(defaultState.counters),
      ...(state?.counters || {})
    }
  };

  for (const key of ['users', 'equipment', 'history', 'body_parts', 'difficulty_levels', 'exercises', 'exercise_videos']) {
    if (!Array.isArray(normalized[key])) {
      normalized[key] = [];
      changed = true;
    }
  }

  const counterTargets = {
    users: maxId(normalized.users),
    equipment: maxId(normalized.equipment),
    history: maxId(normalized.history),
    body_parts: maxId(normalized.body_parts),
    difficulty_levels: maxId(normalized.difficulty_levels),
    exercises: maxId(normalized.exercises),
    exercise_videos: maxId(normalized.exercise_videos)
  };
  for (const [key, value] of Object.entries(counterTargets)) {
    if (!Number.isInteger(normalized.counters[key]) || normalized.counters[key] < value) {
      normalized.counters[key] = value;
      changed = true;
    }
  }

  const existingNames = new Set(normalized.equipment.map(item => item.name));
  const labels = await loadLabelEquipment();
  for (const labelEquipment of labels) {
    if (!existingNames.has(labelEquipment.name)) {
      const id = nextId(normalized, 'equipment');
      normalized.equipment.push({
        ...labelEquipment,
        id
      });
      existingNames.add(labelEquipment.name);
      changed = true;
    }
  }

  // Seed knowledge base data if body_parts is empty
  if (normalized.body_parts.length === 0) {
    seedKnowledgeBase(normalized);
    changed = true;
  }

  return { state: normalized, changed };
}

function seedKnowledgeBase(state) {
  // Body parts
  const bodyPartNames = ['肩', '腿', '腘绳肌', '股四头肌', '核心'];
  for (const name of bodyPartNames) {
    state.body_parts.push({
      id: nextId(state, 'body_parts'),
      name,
      created_at: new Date().toISOString()
    });
  }

  // Difficulty levels
  const difficulties = [
    { name: '新手', description: '适合刚开始健身的人群，动作简单安全' },
    { name: '进阶', description: '有一定训练基础的人群，需要更好的肌肉控制' },
    { name: '康复慎用', description: '有相关伤病史的人群需在专业指导下进行' }
  ];
  for (const { name, description } of difficulties) {
    state.difficulty_levels.push({
      id: nextId(state, 'difficulty_levels'),
      name,
      description,
      created_at: new Date().toISOString()
    });
  }

  // Find equipment IDs by name
  const eqMap = new Map(state.equipment.map(e => [e.name, e.id]));
  const shoulderPressId = eqMap.get('坐姿推肩');
  const legExtId = eqMap.get('腿屈伸');
  const legCurlId = eqMap.get('腿弯曲');

  // Exercises seed data
  const exerciseSeeds = [];
  if (shoulderPressId) {
    exerciseSeeds.push(
      {
        equipment_id: shoulderPressId,
        name: '坐姿推肩 - 标准推举',
        steps: '调整座椅高度使握把与肩同高。双手握住握把手心朝前，挺胸收腹，呼气向上推至手臂伸直但不锁肘，吸气缓慢下放至起始位。',
        common_mistakes: '腰部过度前凸借力；推举时耸肩导致斜方肌代偿；下放幅度过大伤肩关节。',
        safety_tips: '调整座椅使腰部紧贴靠背；推举全程保持核心收紧；肩部有伤者应在康复师指导下使用。',
        target_audience: '所有健身者',
        difficulty_level_id: 1,
        body_part_ids: [1, 5]
      },
      {
        equipment_id: shoulderPressId,
        name: '坐姿推肩 - 颈后推举',
        steps: '调整座椅，双手握住握把置于头部后方。呼气向上推举至手臂伸直，吸气缓慢下放至起始位。注意全程控制速度。',
        common_mistakes: '下放过深增加肩关节压力；速度过快失去控制；重量过大导致动作变形。',
        safety_tips: '仅适合肩关节灵活性好的训练者；有肩部伤病者禁用；建议使用较轻重量。',
        target_audience: '进阶训练者',
        difficulty_level_id: 3,
        body_part_ids: [1, 5]
      }
    );
  }
  if (legExtId) {
    exerciseSeeds.push(
      {
        equipment_id: legExtId,
        name: '腿屈伸 - 标准训练',
        steps: '坐于器械上，背部紧贴靠垫，脚踝勾住滚轴。呼气发力将小腿向前踢直至膝关节完全伸展，顶峰收缩1秒，吸气缓慢下放。',
        common_mistakes: '臀部离开座椅借力；下放速度过快；膝关节超伸。',
        safety_tips: '调整靠垫使膝关节与器械旋转轴对齐；膝关节有伤者减少训练幅度。',
        target_audience: '所有健身者',
        difficulty_level_id: 1,
        body_part_ids: [4]
      },
      {
        equipment_id: legExtId,
        name: '腿屈伸 - 单腿训练',
        steps: '坐于器械上，单脚踝勾住滚轴，另一条腿放松。呼气单腿发力伸展膝关节至完全伸直，顶峰收缩1秒后吸气缓慢下放。双腿交替进行。',
        common_mistakes: '身体侧倾代偿；未保持骨盆中立位；两侧训练量不均衡。',
        safety_tips: '先从较轻重量开始适应单腿发力；保持骨盆稳定不偏移。',
        target_audience: '进阶训练者',
        difficulty_level_id: 2,
        body_part_ids: [4]
      }
    );
  }
  if (legCurlId) {
    exerciseSeeds.push(
      {
        equipment_id: legCurlId,
        name: '腿弯曲 - 俯卧腿弯举',
        steps: '俯卧于器械上，脚踝勾住滚轴。吸气发力将小腿向臀部弯举至最高点，顶峰收缩1秒，缓慢下放至起始位。保持髋部贴紧垫面。',
        common_mistakes: '髋部抬离垫面借力；弯举幅度不足；速度过快靠惯性完成。',
        safety_tips: '调整滚轴位置使膝关节与器械轴心对齐；下背部有伤者需谨慎使用。',
        target_audience: '所有健身者',
        difficulty_level_id: 1,
        body_part_ids: [2, 3]
      },
      {
        equipment_id: legCurlId,
        name: '腿弯曲 - 离心控制训练',
        steps: '与标准俯卧腿弯举起始位相同。向心阶段2秒快速弯举至最高点，离心阶段4秒缓慢下放。强调对下放过程的控制。',
        common_mistakes: '离心阶段下放过快失去控制效果；重量过大无法保持4秒下放；呼吸紊乱。',
        safety_tips: '选择比标准训练轻10-20%的重量；训练后腘绳肌可能较酸痛，注意拉伸恢复。',
        target_audience: '进阶训练者',
        difficulty_level_id: 2,
        body_part_ids: [2, 3]
      }
    );
  }

  for (const seed of exerciseSeeds) {
    const id = nextId(state, 'exercises');
    state.exercises.push({
      id,
      equipment_id: seed.equipment_id,
      name: seed.name,
      steps: seed.steps,
      common_mistakes: seed.common_mistakes,
      safety_tips: seed.safety_tips,
      target_audience: seed.target_audience,
      difficulty_level_id: seed.difficulty_level_id,
      body_part_ids: seed.body_part_ids,
      created_at: new Date().toISOString()
    });
  }

  // Exercise videos seed data
  const videoSeeds = [
    { exercise_id: 1, title: '坐姿推肩标准动作教学', url: 'https://example.com/videos/shoulder_press_standard.mp4', duration: '3:24' },
    { exercise_id: 2, title: '坐姿颈后推肩教学', url: 'https://example.com/videos/shoulder_press_behind_neck.mp4', duration: '4:10' },
    { exercise_id: 3, title: '腿屈伸标准动作教学', url: 'https://example.com/videos/leg_extension_standard.mp4', duration: '2:55' },
    { exercise_id: 4, title: '单腿腿屈伸教学', url: 'https://example.com/videos/leg_extension_single.mp4', duration: '3:30' },
    { exercise_id: 5, title: '俯卧腿弯举标准教学', url: 'https://example.com/videos/leg_curl_prone.mp4', duration: '3:15' },
    { exercise_id: 6, title: '腿弯举离心训练教学', url: 'https://example.com/videos/leg_curl_eccentric.mp4', duration: '4:00' }
  ];
  for (const seed of videoSeeds) {
    // Only add video if the exercise exists
    if (state.exercises.some(e => e.id === seed.exercise_id)) {
      state.exercise_videos.push({
        id: nextId(state, 'exercise_videos'),
        exercise_id: seed.exercise_id,
        title: seed.title,
        url: seed.url,
        duration: seed.duration,
        created_at: new Date().toISOString()
      });
    }
  }
}

async function readStateFile() {
  const content = await fs.readFile(dataPath, 'utf-8');
  return JSON.parse(content);
}

async function readState() {
  await ensureReady();
  return readStateFile();
}

async function writeState(state) {
  await fs.writeFile(tempDataPath, JSON.stringify(state, null, 2), 'utf-8');
  await fs.rename(tempDataPath, dataPath);
}

async function withStateMutation(mutator) {
  const run = mutationQueue.then(async () => {
    await ensureReady();
    const state = await readStateFile();
    const result = await mutator(state);
    await writeState(state);
    return result;
  });
  mutationQueue = run.catch(() => {});
  return run;
}

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function nextId(state, key) {
  state.counters[key] += 1;
  return state.counters[key];
}

export async function pingStorage() {
  await ensureReady();
  return { driver: 'file' };
}

export async function listEquipment({ q, targetMuscle, limit, offset }) {
  const state = await readState();
  let rows = state.equipment.slice();

  if (q) {
    const keyword = q.toLowerCase();
    rows = rows.filter(item =>
      [item.name, item.description, item.tutorial_text].some(value =>
        typeof value === 'string' && value.toLowerCase().includes(keyword)
      )
    );
  }

  if (targetMuscle) {
    rows = rows.filter(item => item.target_muscle === targetMuscle);
  }

  rows.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'));
  const total = rows.length;
  return {
    total,
    rows: clone(rows.slice(offset, offset + limit))
  };
}

export async function listEquipmentTargets() {
  const state = await readState();
  return [...new Set(state.equipment.map(item => item.target_muscle).filter(Boolean))].sort((a, b) =>
    a.localeCompare(b, 'zh-CN')
  );
}

export async function getEquipmentById(id) {
  const state = await readState();
  return clone(state.equipment.find(item => item.id === id) || null);
}

export async function getEquipmentByName(name) {
  const state = await readState();
  return clone(state.equipment.find(item => item.name === name) || null);
}

export async function createEquipment(payload) {
  return withStateMutation(state => {
    const id = nextId(state, 'equipment');
    const record = {
      id,
      name: payload.name,
      description: payload.description ?? null,
      target_muscle: payload.target_muscle ?? null,
      tutorial_text: payload.tutorial_text ?? null,
      tutorial_url: payload.tutorial_url ?? null,
      category: payload.category ?? null,
      suitable_for: payload.suitable_for ?? null,
      created_at: new Date().toISOString()
    };
    state.equipment.push(record);
    return clone(record);
  });
}

export async function updateEquipment(id, payload) {
  return withStateMutation(state => {
    const index = state.equipment.findIndex(item => item.id === id);
    if (index < 0) {
      return null;
    }
    state.equipment[index] = {
      ...state.equipment[index],
      ...payload
    };
    return clone(state.equipment[index]);
  });
}

export async function deleteEquipment(id) {
  return withStateMutation(state => {
    const before = state.equipment.length;
    state.equipment = state.equipment.filter(item => item.id !== id);
    state.history = state.history.filter(item => item.equipment_id !== id);
    // Cascade delete exercises and their videos
    const deletedExerciseIds = state.exercises
      .filter(ex => ex.equipment_id === id)
      .map(ex => ex.id);
    state.exercises = state.exercises.filter(ex => ex.equipment_id !== id);
    if (deletedExerciseIds.length > 0) {
      const deletedSet = new Set(deletedExerciseIds);
      state.exercise_videos = state.exercise_videos.filter(v => !deletedSet.has(v.exercise_id));
    }
    return state.equipment.length !== before;
  });
}

export async function findUserByEmail(email) {
  const state = await readState();
  return clone(state.users.find(user => user.email === email) || null);
}

export async function findUserById(id) {
  const state = await readState();
  return clone(state.users.find(user => user.id === id) || null);
}

export async function createUser({ email, password_hash, nickname }) {
  return withStateMutation(state => {
    const id = nextId(state, 'users');
    const record = {
      id,
      email,
      password_hash,
      nickname: nickname ?? null,
      created_at: new Date().toISOString()
    };
    state.users.push(record);
    return clone(record);
  });
}

export async function updateUser(id, payload) {
  return withStateMutation(state => {
    const index = state.users.findIndex(user => user.id === id);
    if (index < 0) {
      return null;
    }
    state.users[index] = {
      ...state.users[index],
      ...payload
    };
    return clone(state.users[index]);
  });
}

export async function createHistory({ user_id, equipment_id, scanned_at }) {
  return withStateMutation(state => {
    const id = nextId(state, 'history');
    const record = {
      id,
      user_id,
      equipment_id,
      scanned_at: scanned_at instanceof Date ? scanned_at.toISOString() : scanned_at
    };
    state.history.push(record);
    return clone(record);
  });
}

export async function listHistory({ userId, limit, offset }) {
  const state = await readState();
  const equipmentMap = new Map(state.equipment.map(item => [item.id, item]));
  const filtered = state.history
    .filter(item => item.user_id === userId)
    .sort((a, b) => String(b.scanned_at).localeCompare(String(a.scanned_at)));
  const total = filtered.length;
  const rows = filtered.slice(offset, offset + limit).map(item => ({
    ...item,
    equipment: equipmentMap.get(item.equipment_id) || null
  }));
  return { total, rows: clone(rows) };
}

export async function listHistoryAll(userId) {
  const state = await readState();
  const equipmentMap = new Map(state.equipment.map(item => [item.id, item]));
  return clone(
    state.history
      .filter(item => item.user_id === userId)
      .sort((a, b) => String(b.scanned_at).localeCompare(String(a.scanned_at)))
      .map(item => ({
        ...item,
        equipment: equipmentMap.get(item.equipment_id) || null
      }))
  );
}

export async function deleteHistory(id, userId) {
  return withStateMutation(state => {
    const target = state.history.find(item => item.id === id);
    if (!target) {
      return { found: false, deleted: false };
    }
    if (target.user_id !== userId) {
      return { found: true, deleted: false };
    }
    state.history = state.history.filter(item => item.id !== id);
    return { found: true, deleted: true };
  });
}

export async function getHistoryStats(userId) {
  const state = await readState();
  const history = state.history.filter(item => item.user_id === userId);
  const counts = new Map();
  for (const item of history) {
    counts.set(item.equipment_id, (counts.get(item.equipment_id) || 0) + 1);
  }
  const equipmentMap = new Map(state.equipment.map(item => [item.id, item]));
  const topEquipment = [...counts.entries()]
    .map(([equipmentId, count]) => ({
      name: equipmentMap.get(equipmentId)?.name || `equipment-${equipmentId}`,
      count
    }))
    .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'zh-CN'))
    .slice(0, 5);

  return {
    totalScans: history.length,
    uniqueEquipment: counts.size,
    topEquipment
  };
}

// ============ Body Parts ============

export async function listBodyParts() {
  const state = await readState();
  return clone(state.body_parts.slice().sort((a, b) => a.id - b.id));
}

export async function getBodyPartById(id) {
  const state = await readState();
  return clone(state.body_parts.find(item => item.id === id) || null);
}

export async function getBodyPartByName(name) {
  const state = await readState();
  return clone(state.body_parts.find(item => item.name === name) || null);
}

export async function createBodyPart(payload) {
  return withStateMutation(state => {
    if (state.body_parts.some(item => item.name === payload.name)) {
      const err = new Error('Body part name already exists');
      err.statusCode = 409;
      err.code = 'DUPLICATE_ENTRY';
      throw err;
    }
    const id = nextId(state, 'body_parts');
    const record = { id, name: payload.name, created_at: new Date().toISOString() };
    state.body_parts.push(record);
    return clone(record);
  });
}

export async function updateBodyPart(id, payload) {
  return withStateMutation(state => {
    const index = state.body_parts.findIndex(item => item.id === id);
    if (index < 0) return null;
    if (payload.name && state.body_parts.some(item => item.name === payload.name && item.id !== id)) {
      const err = new Error('Body part name already exists');
      err.statusCode = 409;
      err.code = 'DUPLICATE_ENTRY';
      throw err;
    }
    state.body_parts[index] = { ...state.body_parts[index], ...payload };
    return clone(state.body_parts[index]);
  });
}

export async function deleteBodyPart(id) {
  return withStateMutation(state => {
    const index = state.body_parts.findIndex(item => item.id === id);
    if (index < 0) return false;
    // Check if any exercise references this body part
    const inUse = state.exercises.some(ex => ex.body_part_ids && ex.body_part_ids.includes(id));
    if (inUse) {
      const err = new Error('Cannot delete: exercises reference this body part');
      err.statusCode = 409;
      err.code = 'IN_USE';
      throw err;
    }
    state.body_parts.splice(index, 1);
    return true;
  });
}

export async function bodyPartsExist(ids) {
  const state = await readState();
  const idSet = new Set(state.body_parts.map(bp => bp.id));
  return ids.every(id => idSet.has(id));
}

// ============ Difficulty Levels ============

export async function listDifficultyLevels() {
  const state = await readState();
  return clone(state.difficulty_levels.slice().sort((a, b) => a.id - b.id));
}

export async function getDifficultyLevelById(id) {
  const state = await readState();
  return clone(state.difficulty_levels.find(item => item.id === id) || null);
}

export async function getDifficultyLevelByName(name) {
  const state = await readState();
  return clone(state.difficulty_levels.find(item => item.name === name) || null);
}

export async function createDifficultyLevel(payload) {
  return withStateMutation(state => {
    if (state.difficulty_levels.some(item => item.name === payload.name)) {
      const err = new Error('Difficulty level name already exists');
      err.statusCode = 409;
      err.code = 'DUPLICATE_ENTRY';
      throw err;
    }
    const id = nextId(state, 'difficulty_levels');
    const record = {
      id,
      name: payload.name,
      description: payload.description ?? null,
      created_at: new Date().toISOString()
    };
    state.difficulty_levels.push(record);
    return clone(record);
  });
}

export async function updateDifficultyLevel(id, payload) {
  return withStateMutation(state => {
    const index = state.difficulty_levels.findIndex(item => item.id === id);
    if (index < 0) return null;
    if (payload.name && state.difficulty_levels.some(item => item.name === payload.name && item.id !== id)) {
      const err = new Error('Difficulty level name already exists');
      err.statusCode = 409;
      err.code = 'DUPLICATE_ENTRY';
      throw err;
    }
    state.difficulty_levels[index] = { ...state.difficulty_levels[index], ...payload };
    return clone(state.difficulty_levels[index]);
  });
}

export async function deleteDifficultyLevel(id) {
  return withStateMutation(state => {
    const index = state.difficulty_levels.findIndex(item => item.id === id);
    if (index < 0) return false;
    const inUse = state.exercises.some(ex => ex.difficulty_level_id === id);
    if (inUse) {
      const err = new Error('Cannot delete: exercises reference this difficulty level');
      err.statusCode = 409;
      err.code = 'IN_USE';
      throw err;
    }
    state.difficulty_levels.splice(index, 1);
    return true;
  });
}

// ============ Exercises ============

export async function listExercises({ q, equipmentId, difficultyLevelId, bodyPartId, limit, offset }) {
  const state = await readState();
  let rows = state.exercises.slice();

  if (equipmentId) {
    rows = rows.filter(item => item.equipment_id === equipmentId);
  }
  if (difficultyLevelId) {
    rows = rows.filter(item => item.difficulty_level_id === difficultyLevelId);
  }
  if (bodyPartId) {
    rows = rows.filter(item => item.body_part_ids && item.body_part_ids.includes(bodyPartId));
  }
  if (q) {
    const keyword = q.toLowerCase();
    rows = rows.filter(item =>
      [item.name, item.steps, item.common_mistakes, item.safety_tips, item.target_audience].some(
        value => typeof value === 'string' && value.toLowerCase().includes(keyword)
      )
    );
  }

  rows.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'));
  const total = rows.length;
  return { total, rows: clone(rows.slice(offset, offset + limit)) };
}

export async function getExerciseById(id) {
  const state = await readState();
  return clone(state.exercises.find(item => item.id === id) || null);
}

export async function getExercisesByEquipmentId(equipmentId) {
  const state = await readState();
  return clone(state.exercises.filter(item => item.equipment_id === equipmentId));
}

export async function createExercise(payload) {
  return withStateMutation(state => {
    // Validate equipment_id
    if (!state.equipment.some(e => e.id === payload.equipment_id)) {
      const err = new Error('Referenced equipment not found');
      err.statusCode = 400;
      err.code = 'VALIDATION_ERROR';
      throw err;
    }
    // Validate difficulty_level_id
    if (payload.difficulty_level_id != null && !state.difficulty_levels.some(d => d.id === payload.difficulty_level_id)) {
      const err = new Error('Referenced difficulty level not found');
      err.statusCode = 400;
      err.code = 'VALIDATION_ERROR';
      throw err;
    }
    // Validate body_part_ids
    if (payload.body_part_ids && payload.body_part_ids.length > 0) {
      const bpIdSet = new Set(state.body_parts.map(bp => bp.id));
      const invalid = payload.body_part_ids.filter(id => !bpIdSet.has(id));
      if (invalid.length > 0) {
        const err = new Error(`Referenced body parts not found: ${invalid.join(', ')}`);
        err.statusCode = 400;
        err.code = 'VALIDATION_ERROR';
        throw err;
      }
    }
    const id = nextId(state, 'exercises');
    const record = {
      id,
      equipment_id: payload.equipment_id,
      name: payload.name,
      steps: payload.steps,
      common_mistakes: payload.common_mistakes ?? null,
      safety_tips: payload.safety_tips ?? null,
      target_audience: payload.target_audience ?? null,
      difficulty_level_id: payload.difficulty_level_id ?? null,
      body_part_ids: payload.body_part_ids ?? [],
      created_at: new Date().toISOString()
    };
    state.exercises.push(record);
    return clone(record);
  });
}

export async function updateExercise(id, payload) {
  return withStateMutation(state => {
    const index = state.exercises.findIndex(item => item.id === id);
    if (index < 0) return null;
    if (payload.equipment_id != null && !state.equipment.some(e => e.id === payload.equipment_id)) {
      const err = new Error('Referenced equipment not found');
      err.statusCode = 400;
      err.code = 'VALIDATION_ERROR';
      throw err;
    }
    if (payload.difficulty_level_id != null && !state.difficulty_levels.some(d => d.id === payload.difficulty_level_id)) {
      const err = new Error('Referenced difficulty level not found');
      err.statusCode = 400;
      err.code = 'VALIDATION_ERROR';
      throw err;
    }
    if (payload.body_part_ids && payload.body_part_ids.length > 0) {
      const bpIdSet = new Set(state.body_parts.map(bp => bp.id));
      const invalid = payload.body_part_ids.filter(bid => !bpIdSet.has(bid));
      if (invalid.length > 0) {
        const err = new Error(`Referenced body parts not found: ${invalid.join(', ')}`);
        err.statusCode = 400;
        err.code = 'VALIDATION_ERROR';
        throw err;
      }
    }
    state.exercises[index] = { ...state.exercises[index], ...payload };
    return clone(state.exercises[index]);
  });
}

export async function deleteExercise(id) {
  return withStateMutation(state => {
    const before = state.exercises.length;
    state.exercises = state.exercises.filter(item => item.id !== id);
    state.exercise_videos = state.exercise_videos.filter(v => v.exercise_id !== id);
    return state.exercises.length !== before;
  });
}

// ============ Exercise Videos ============

export async function listExerciseVideos(exerciseId) {
  const state = await readState();
  return clone(state.exercise_videos.filter(v => v.exercise_id === exerciseId));
}

export async function getExerciseVideoById(id) {
  const state = await readState();
  return clone(state.exercise_videos.find(item => item.id === id) || null);
}

export async function createExerciseVideo(payload) {
  return withStateMutation(state => {
    if (!state.exercises.some(e => e.id === payload.exercise_id)) {
      const err = new Error('Referenced exercise not found');
      err.statusCode = 400;
      err.code = 'VALIDATION_ERROR';
      throw err;
    }
    const id = nextId(state, 'exercise_videos');
    const record = {
      id,
      exercise_id: payload.exercise_id,
      title: payload.title,
      url: payload.url,
      duration: payload.duration ?? null,
      created_at: new Date().toISOString()
    };
    state.exercise_videos.push(record);
    return clone(record);
  });
}

export async function updateExerciseVideo(id, payload) {
  return withStateMutation(state => {
    const index = state.exercise_videos.findIndex(item => item.id === id);
    if (index < 0) return null;
    if (payload.exercise_id != null && !state.exercises.some(e => e.id === payload.exercise_id)) {
      const err = new Error('Referenced exercise not found');
      err.statusCode = 400;
      err.code = 'VALIDATION_ERROR';
      throw err;
    }
    state.exercise_videos[index] = { ...state.exercise_videos[index], ...payload };
    return clone(state.exercise_videos[index]);
  });
}

export async function deleteExerciseVideo(id) {
  return withStateMutation(state => {
    const before = state.exercise_videos.length;
    state.exercise_videos = state.exercise_videos.filter(item => item.id !== id);
    return state.exercise_videos.length !== before;
  });
}
