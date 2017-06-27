FROM alpine

RUN apk update

RUN apk add nginx python supervisor uwsgi uwsgi-python

EXPOSE 80

RUN mkdir -p /run/nginx
RUN echo "daemon off;" >> /etc/nginx/nginx.conf
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/

# UWSGI config
COPY loris2.wsgi /var/www/loris2/loris2.wsgi
COPY uwsgi.ini /etc/uwsgi

# supervisord config
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]


# FROM ubuntu:14.04
#
# # https://stackoverflow.com/a/23570150/1558022
# ENV DEBIAN_FRONTEND noninteractive
#
# RUN apt-get update
# RUN apt-get install -y wget
# RUN apt-get install -y unzip
# RUN wget https://github.com/loris-imageserver/loris/archive/v2.1.0-final.zip
# RUN unzip v2.1.0-final.zip
#
# WORKDIR loris-2.1.0-final
# RUN apt-get install -y python
# RUN apt-get install -y python-setuptools
#
# # User "loris" and or group "loris" do(es) not exist.
# # Please create this user, e.g.:
# #     `useradd -d /var/www/loris -s /sbin/false loris`
# RUN useradd -d /var/www/loris -s /sbin/false loris
#
# RUN apt-get install -y zlib1g-dev
# RUN apt-get install -y libjpeg-turbo8-dev
# RUN apt-get install -y python-dev
# RUN apt-get install -y python-pip
#
# RUN pip install configobj
# RUN python setup.py install
#
# COPY loris2.wsgi /var/www/loris2/loris2.wsgi
#
# RUN apt-get install -y nginx
#
# RUN apt-get install -y supervisor
# RUN apt-get install -y libpcre3 libpcre3-dev
# RUN pip install uwsgi
#
# EXPOSE 80
# RUN mkdir -p /etc/nginx
# RUN echo "daemon off;" >> /etc/nginx/nginx.conf
# RUN rm -f /etc/nginx/conf.d/default.conf
# COPY nginx.conf /etc/nginx/conf.d/
#
# COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
#
# RUN useradd -s /sbin/false nginx
#
# COPY uwsgi.ini /etc/uwsgi/
#
# COPY loris2.conf /opt/loris/etc/loris2.conf
#
# CMD ["/usr/bin/supervisord"]
