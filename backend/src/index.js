import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import userRouter from './routes/user.js';
import equipmentRouter from './routes/equipment.js';
import historyRouter from './routes/history.js';
import bodyPartsRouter from './routes/bodyParts.js';
import difficultyLevelsRouter from './routes/difficultyLevels.js';
import exercisesRouter from './routes/exercises.js';
import exerciseVideosRouter from './routes/exerciseVideos.js';
import config from './config.js';
import { errorHandler, notFoundHandler } from './middleware/error-handler.js';
import { pingStorage } from './store.js';

const app = express();

app.use(express.json({ limit: '1mb' }));

app.use((req, res, next) => {
  const startedAt = Date.now();
  res.on('finish', () => {
    const elapsedMs = Date.now() - startedAt;
    console.log(`${new Date().toISOString()} ${res.statusCode} ${req.method} ${req.originalUrl} ${elapsedMs}ms`);
  });
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

app.get('/', (_req, res) => {
  res.json({
    ok: true,
    name: 'GymEye backend',
    health: '/api/health'
  });
});

app.use('/api/user', userRouter);
app.use('/api/equipment', equipmentRouter);
app.use('/api/history', historyRouter);
app.use('/api/body-parts', bodyPartsRouter);
app.use('/api/difficulty-levels', difficultyLevelsRouter);
app.use('/api/exercises', exercisesRouter);
app.use('/api/exercise-videos', exerciseVideosRouter);

app.use(notFoundHandler);
app.use(errorHandler);

app.listen(config.port, () => {
  console.log(`GymEye backend listening on port ${config.port}`);
});
