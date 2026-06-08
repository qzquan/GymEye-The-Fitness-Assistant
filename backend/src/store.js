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
  workout_logs: [],
  exercise_videos: [],
  counters: {
    users: 0,
    equipment: 0,
    history: 0,
    workout_logs: 0,
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
  await writeState(initialState);
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

  for (const key of ['users', 'equipment', 'history', 'workout_logs', 'exercise_videos']) {
    if (!Array.isArray(normalized[key])) {
      normalized[key] = [];
      changed = true;
    }
    // Also write back if state was missing this key (so it gets persisted)
    if (!Array.isArray(state?.[key])) {
      changed = true;
    }
  }

  const counterTargets = {
    users: maxId(normalized.users),
    equipment: maxId(normalized.equipment),
    history: maxId(normalized.history),
    workout_logs: maxId(normalized.workout_logs || []),
    exercise_videos: maxId(normalized.exercise_videos || [])
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

  return { state: normalized, changed };
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

export async function createWorkoutLog(userId, payload) {
  return withStateMutation(state => {
    const id = nextId(state, 'workout_logs');
    const record = {
      id,
      user_id: userId,
      exercise_name: payload.exercise_name,
      sets: payload.sets ?? null,
      reps_per_set: payload.reps_per_set ?? null,
      weight_kg: payload.weight_kg ?? null,
      duration_minutes: payload.duration_minutes ?? null,
      feeling: payload.feeling,
      body_part: payload.body_part ?? null,
      performed_at: payload.performed_at,
      created_at: new Date().toISOString()
    };
    state.workout_logs.push(record);
    return clone(record);
  });
}

export async function listWorkoutLogs({ userId, limit, offset, from, to }) {
  const state = await readState();
  let rows = (state.workout_logs || [])
    .filter(item => item.user_id === userId)
    .sort((a, b) => String(b.performed_at).localeCompare(String(a.performed_at)));

  if (from) {
    rows = rows.filter(item => item.performed_at >= from);
  }
  if (to) {
    const toEnd = to + 'T23:59:59.999Z';
    rows = rows.filter(item => item.performed_at <= toEnd);
  }

  const total = rows.length;
  return {
    total,
    rows: clone(rows.slice(offset, offset + limit))
  };
}

export async function getWorkoutStats(userId) {
  const state = await readState();
  const logs = (state.workout_logs || []).filter(item => item.user_id === userId);

  // Total workouts count
  const totalWorkouts = logs.length;

  // Total duration
  const totalDurationMinutes = logs.reduce((sum, item) => sum + (item.duration_minutes || 0), 0);

  // Most trained body part
  const bodyPartCounts = new Map();
  for (const item of logs) {
    if (item.body_part) {
      bodyPartCounts.set(item.body_part, (bodyPartCounts.get(item.body_part) || 0) + 1);
    }
  }
  const topBodyParts = [...bodyPartCounts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name, count]) => ({ name, count }));

  // Consecutive check-in days (streak)
  const uniqueDays = new Set(
    logs.map(item => String(item.performed_at).slice(0, 10))
  );
  const sortedDays = [...uniqueDays].sort().reverse();
  let streak = 0;
  const today = new Date().toISOString().slice(0, 10);
  let expected = today;
  for (const day of sortedDays) {
    if (day === expected) {
      streak++;
      const d = new Date(expected);
      d.setDate(d.getDate() - 1);
      expected = d.toISOString().slice(0, 10);
    } else if (streak === 0) {
      // Start streak from most recent day if today has no workout
      expected = day;
      streak = 1;
      const d = new Date(expected);
      d.setDate(d.getDate() - 1);
      expected = d.toISOString().slice(0, 10);
    } else {
      break;
    }
  }

  // Weekly training count (this week Mon-Sun)
  const now = new Date();
  const dayOfWeek = now.getDay();
  const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
  const monday = new Date(now);
  monday.setDate(now.getDate() + mondayOffset);
  const mondayStr = monday.toISOString().slice(0, 10);
  const weeklyCount = logs.filter(item =>
    String(item.performed_at).slice(0, 10) >= mondayStr
  ).length;

  // Weight progression data (grouped by exercise, sorted by date)
  const weightProgression = new Map();
  for (const item of logs) {
    if (item.weight_kg == null) continue;
    const key = item.exercise_name;
    if (!weightProgression.has(key)) {
      weightProgression.set(key, []);
    }
    weightProgression.get(key).push({
      date: String(item.performed_at).slice(0, 10),
      weight: item.weight_kg
    });
  }
  for (const entries of weightProgression.values()) {
    entries.sort((a, b) => a.date.localeCompare(b.date));
  }
  const weightProgressionData = Object.fromEntries(weightProgression);

  // Training calendar (last 90 days)
  const calendarDays = [];
  for (let i = 89; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    const dateStr = d.toISOString().slice(0, 10);
    const count = logs.filter(item =>
      String(item.performed_at).slice(0, 10) === dateStr
    ).length;
    calendarDays.push({ date: dateStr, count });
  }

  return {
    totalWorkouts,
    totalDurationMinutes,
    topBodyParts,
    streak,
    weeklyCount,
    weightProgression: weightProgressionData,
    calendarDays
  };
}

export async function deleteWorkoutLog(id, userId) {
  return withStateMutation(state => {
    const index = (state.workout_logs || []).findIndex(item => item.id === id);
    if (index < 0) {
      return { found: false, deleted: false };
    }
    if (state.workout_logs[index].user_id !== userId) {
      return { found: true, deleted: false };
    }
    state.workout_logs.splice(index, 1);
    return { found: true, deleted: true };
  });
}

/* ── Exercise Videos ─────────────────────────────────────── */

export async function listExerciseVideos(exerciseId) {
  const state = await readState();
  const rows = (state.exercise_videos || [])
    .filter(item => item.exercise_id === exerciseId);
  return clone(rows);
}

export async function getExerciseVideoById(id) {
  const state = await readState();
  return clone((state.exercise_videos || []).find(item => item.id === id) || null);
}

export async function createExerciseVideo(data) {
  return withStateMutation(state => {
    if (!Array.isArray(state.exercise_videos)) {
      state.exercise_videos = [];
    }
    const id = nextId(state, 'exercise_videos');
    const record = {
      id,
      exercise_id: data.exercise_id,
      title: data.title,
      url: data.url,
      duration: data.duration ?? null,
      created_at: new Date().toISOString()
    };
    state.exercise_videos.push(record);
    return clone(record);
  });
}

export async function updateExerciseVideo(id, patch) {
  return withStateMutation(state => {
    const videos = state.exercise_videos || [];
    const index = videos.findIndex(item => item.id === id);
    if (index < 0) return null;
    videos[index] = { ...videos[index], ...patch };
    return clone(videos[index]);
  });
}

export async function deleteExerciseVideo(id) {
  return withStateMutation(state => {
    const before = (state.exercise_videos || []).length;
    state.exercise_videos = (state.exercise_videos || []).filter(item => item.id !== id);
    return (state.exercise_videos || []).length !== before;
  });
}
