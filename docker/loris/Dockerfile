FROM alpine

COPY install_loris.sh /install_loris.sh
RUN /install_loris.sh

# Install nginx and UWSGI dependencies
RUN apk update && \
		apk add nginx supervisor uwsgi uwsgi-python && \
		rm -rf /var/cache/apk/*

# Set up the nginx config
RUN mkdir -p /run/nginx
RUN echo "daemon off;" >> /etc/nginx/nginx.conf
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/

# Copy in other config files
COPY loris2.wsgi /var/www/loris2/loris2.wsgi
COPY loris2.conf /opt/loris/etc/loris2.conf
COPY uwsgi.ini /etc/uwsgi
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

EXPOSE 80

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
