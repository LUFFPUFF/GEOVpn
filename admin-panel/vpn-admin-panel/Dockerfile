FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install && npm install -D esbuild
COPY . .
COPY .env* ./ 
RUN npx vite build --base=/admin/

FROM nginx:1.25-alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
