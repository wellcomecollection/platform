FROM ubuntu:16.04


# Install base Python and uWSGI (web server) dependencies
RUN apt-get update && \
    apt-get install --yes python3 python3-pip uwsgi uwsgi-plugin-python3 && \
    apt-get clean

RUN pip3 install --upgrade pip

# Imaging dependencies.  These libraries don't change much between versions
# of Loris, so we can cache their installation.
RUN apt-get update && \
    apt-get install --yes libffi-dev libjpeg-turbo8-dev libfreetype6-dev zlib1g-dev \
        liblcms2-dev liblcms2-utils libssl-dev libtiff5-dev libwebp-dev && \
    apt-get clean


ENV LORIS_GITHUB_USER loris-imageserver
ENV LORIS_COMMIT 841383810cdd62ae591f532fd89330bb0be06565

COPY requirements.txt /
COPY install_loris.sh /install_loris.sh
RUN /install_loris.sh


# This is an sRGB color profile downloaded from
# http://www.color.org/srgbprofiles.xalter
COPY sRGB2014.icc /usr/share/color/icc/colord/sRGB2014.icc

ENV LORIS_CONF_FILE /opt/loris/etc/loris2.conf

COPY loris2.conf /opt/loris/etc/loris2.conf
COPY loris2.wsgi /var/www/loris2/loris2.wsgi
COPY uwsgi.ini /etc/uwsgi

EXPOSE 8888

CMD ["/usr/bin/uwsgi", "--ini", "/etc/uwsgi/uwsgi.ini"]
