import 'dotenv/config';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import pool from '../../src/db.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function main() {
  const labelsPath = path.resolve(__dirname, '../../../android_project/app/src/main/assets/label.txt');
  const content = fs.readFileSync(labelsPath, 'utf-8');
  const lines = content.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
  for (const name of lines) {
    try {
      await pool.query('INSERT IGNORE INTO equipment (name) VALUES (?)', [name]);
    } catch {}
  }
  process.exit(0);
}

main();

