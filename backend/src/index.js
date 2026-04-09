import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import userRouter from './routes/user.js';
import equipmentRouter from './routes/equipment.js';
import historyRouter from './routes/history.js';

const app = express();
app.use(express.json());

// 添加请求日志中间件，方便调试
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.url}`);
  console.log('Headers:', JSON.stringify(req.headers));
  if (req.method === 'POST') {
    console.log('Body:', JSON.stringify(req.body));
  }
  next();
});

app.use(
  cors({
    origin: process.env.CORS_ORIGIN || '*'
  })
);

app.use('/api/user', userRouter);
app.use('/api/equipment', equipmentRouter);
app.use('/api/history', historyRouter);

app.get('/api/health', (_, res) => {
  res.json({ ok: true });
});

const port = Number(process.env.PORT || 8080);
app.listen(port, () => {});

