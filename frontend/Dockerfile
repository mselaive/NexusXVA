FROM node:22-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS builder

WORKDIR /app

ARG NEXUSXVA_API_BASE_URL=http://backend:8080
ARG BLEMBERG_BASE_URL=http://host.docker.internal:8081
ENV NEXUSXVA_API_BASE_URL=${NEXUSXVA_API_BASE_URL}
ENV BLEMBERG_BASE_URL=${BLEMBERG_BASE_URL}
ENV NEXT_TELEMETRY_DISABLED=1

COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV NEXUSXVA_API_BASE_URL=http://backend:8080
ENV BLEMBERG_BASE_URL=http://host.docker.internal:8081

COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/next.config.ts ./next.config.ts
COPY --from=builder /app/package.json ./package.json
COPY --from=deps /app/node_modules ./node_modules

EXPOSE 3000

CMD ["npm", "run", "start", "--", "-H", "0.0.0.0", "-p", "3000"]
