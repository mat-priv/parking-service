FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
# Expects frontend assets to be built locally first (e.g. npm run build)
COPY dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
