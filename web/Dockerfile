# Tamixa parent web (Vite + React).
# Build context: web/ (repo root is NOT required). Local monorepo:
#   docker compose build web
# Railway: set service Root Directory to `web` (default Dockerfile path: Dockerfile).
# From repo root locally: docker build -f web/Dockerfile web
# Set build arg VITE_API_BASE_URL to your public API origin, e.g. https://your-api.up.railway.app

FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
RUN npm install -g serve@14.2.4
COPY --from=build /app/dist ./dist
ENV NODE_ENV=production
EXPOSE 3000
CMD ["sh", "-c", "serve -s dist -l ${PORT:-3000}"]
