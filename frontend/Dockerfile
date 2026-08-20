# syntax=docker/dockerfile:1

FROM node:24.17.0-alpine3.23 AS build
WORKDIR /workspace

RUN npm install --global pnpm@11.19.0

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . ./
RUN pnpm build

FROM nginxinc/nginx-unprivileged:1.30.4-alpine-slim

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/health || exit 1
