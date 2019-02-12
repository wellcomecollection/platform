FROM nginx:1.15.8-alpine

COPY grafana.nginx.conf /etc/nginx/nginx.conf
