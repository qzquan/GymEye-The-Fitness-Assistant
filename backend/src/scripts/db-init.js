import 'dotenv/config';
import fs from 'fs';
import path from 'path';
import mysql from 'mysql2/promise';

async function main() {
  const {
    DB_HOST = 'localhost',
    DB_PORT = '3306',
    DB_USER = 'root',
    DB_PASSWORD = '',
    DB_NAME = 'gymeye'
  } = process.env;

  const conn = await mysql.createConnection({
    host: DB_HOST,
    port: Number(DB_PORT),
    user: DB_USER,
    password: DB_PASSWORD,
    multipleStatements: true
  });
  await conn.query(`CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`);
  await conn.query(`USE \`${DB_NAME}\`;`);

  const schemaPath = path.resolve(process.cwd(), 'sql', 'schema.sql');
  const sql = fs.readFileSync(schemaPath, 'utf-8');
  await conn.query(sql);
  await conn.end();
  process.exit(0);
}

main().catch(() => process.exit(1));

