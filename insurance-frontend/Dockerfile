# ──────────────────────────────────────────────────────────────────
# Frontend — Vite dev server (test / synthetic-data mode)
#
# Only used by docker compose --profile test.
# The BACKEND_URL env var is picked up by vite.config.ts to proxy
# /api/v1 → the Spring Boot `app` service inside the Docker network.
# ──────────────────────────────────────────────────────────────────
FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .

EXPOSE 5173

# --host 0.0.0.0 makes Vite listen on all interfaces so Docker can
# forward the port to the host machine.
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
