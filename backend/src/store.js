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
  counters: {
    users: 0,
    equipment: 0,
    history: 0
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

  for (const key of ['users', 'equipment', 'history']) {
    if (!Array.isArray(normalized[key])) {
      normalized[key] = [];
      changed = true;
    }
  }

  const counterTargets = {
    users: maxId(normalized.users),
    equipment: maxId(normalized.equipment),
    history: maxId(normalized.history)
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
