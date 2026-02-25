# Stage 1: Build
FROM node:20-alpine AS build

WORKDIR /app

# Install dependencies (only production dependencies are needed for running, but dev dependencies for build if any)
COPY package*.json ./
RUN npm ci --only=production

# Stage 2: Runtime
FROM node:20-alpine

WORKDIR /app

# Copy only necessary files from build stage
COPY --from=build /app/node_modules ./node_modules
COPY . .

# Environment setup
ENV NODE_ENV=production
ENV PORT=3000

# Expose port
EXPOSE 3000

# Start command
CMD ["node", "server.js"]
