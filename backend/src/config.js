const requiredInProduction = ['JWT_SECRET'];

function readConfig() {
  const config = {
    port: Number(process.env.PORT || 8080),
    corsOrigin: process.env.CORS_ORIGIN || '*',
    jwtSecret: process.env.JWT_SECRET || 'secret',
    nodeEnv: process.env.NODE_ENV || 'development'
  };

  if (!Number.isInteger(config.port) || config.port <= 0) {
    throw new Error('PORT must be a positive integer');
  }

  if (config.nodeEnv === 'production') {
    for (const key of requiredInProduction) {
      if (!process.env[key]) {
        throw new Error(`${key} is required in production`);
      }
    }
  }

  return config;
}

const config = readConfig();

export default config;
