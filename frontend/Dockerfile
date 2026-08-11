# syntax=docker/dockerfile:1

# ---- Build stage --------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /app

# Install dependencies first so this layer is cached between builds.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Baked into the production bundle at build time. Defaults to "/api" so
# requests hit this same origin and are handled by the nginx proxy below
# (rather than the "http://localhost:8080" dev default).
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

# ---- Runtime stage -------------------------------------------------------
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
