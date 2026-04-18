import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import userRouter from './routes/user.js';
import equipmentRouter from './routes/equipment.js';
import historyRouter from './routes/history.js';
import config from './config.js';
import { errorHandler, notFoundHandler } from './middleware/error-handler.js';
import { pingStorage } from './store.js';

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
    origin: config.corsOrigin
  })
);

app.get('/api/health', async (_req, res, next) => {
  try {
    const storage = await pingStorage();
    res.json({ ok: true, storage });
  } catch (error) {
    next(error);
  }
});

app.use('/api/user', userRouter);
app.use('/api/equipment', equipmentRouter);
app.use('/api/history', historyRouter);

app.use(notFoundHandler);
app.use(errorHandler);

app.listen(config.port, () => {
  console.log(`GymEye backend listening on port ${config.port}`);
});
