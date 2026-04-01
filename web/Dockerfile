# Tamixa parent web (Vite + React). Build context MUST be the monorepo root:
#   docker build -f web/Dockerfile .
#
# Railway: create a service from this repo, set Root Directory to "." (empty / repository root),
# Docker build path / Dockerfile location: web/Dockerfile.
# Set build variable VITE_API_BASE_URL to your public API origin, e.g. https://tamixa-prod.up.railway.app

FROM node:20-alpine AS build
WORKDIR /app

COPY config ./config
COPY web/package.json web/package-lock.json* ./web/
WORKDIR /app/web
RUN npm ci

COPY web/ ./

# Browser calls this origin for /api/v1 (no trailing slash)
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
RUN npm install -g serve@14.2.4
COPY --from=build /app/web/dist ./dist
ENV NODE_ENV=production
EXPOSE 3000
# Railway sets PORT; serve binds 0.0.0.0 for external access
CMD ["sh", "-c", "serve -s dist -l ${PORT:-3000}"]
