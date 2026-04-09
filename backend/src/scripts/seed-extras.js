import 'dotenv/config';
import mysql from 'mysql2/promise';

const samples = [
  {
    name: '俯卧腿弯举',
    target_muscle: '腘绳肌',
    tutorial_text:
      '俯卧于器械上，脚踝勾住滚轴，吸气发力将小腿向臀部弯举至最高点，顶峰收缩1秒缓慢下放至起始位，整个过程保持髋部贴紧垫面。',
    tutorial_url: 'https://www.bilibili.com/video/BV1f4411a7Ut'
  },
  {
    name: '哑铃卧推',
    target_muscle: '胸大肌',
    tutorial_text:
      '取仰卧位，肩胛收紧下沉，哑铃置于胸外侧，吸气下放至肘部略低于躯干，呼气向上推起于胸上方合拢但不过度锁肘。',
    tutorial_url: 'https://www.bilibili.com/video/BV1k4411a7Uz'
  },
  {
    name: '高位下拉',
    target_muscle: '背阔肌',
    tutorial_text:
      '坐姿挺胸微收下巴，肩胛下沉后将拉杆沿面部前侧下拉至锁骨附近，注意用背部发力，向上还原时保持肌肉张力。',
    tutorial_url: 'https://www.bilibili.com/video/BV1m4411a7Ua'
  }
];

async function main() {
  const conn = await mysql.createConnection({
    host: process.env.DB_HOST || 'localhost',
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'gymeye',
    charset: 'utf8mb4'
  });
  for (const s of samples) {
    await conn.query(
      `INSERT INTO equipment (name, target_muscle, tutorial_text, tutorial_url)
       VALUES (?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         target_muscle = VALUES(target_muscle),
         tutorial_text = VALUES(tutorial_text),
         tutorial_url = VALUES(tutorial_url)`,
      [s.name, s.target_muscle, s.tutorial_text, s.tutorial_url]
    );
  }
  await conn.end();
  process.exit(0);
}

main().catch(() => process.exit(1));
