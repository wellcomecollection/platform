FROM ubuntu:14.04

RUN apt-get update

RUN apt-get install -y python python-pip python-setuptools python-dev \
    uwsgi uwsgi-plugin-python

RUN pip install awscli

ENV LORIS_COMMIT 1e5de1f8b4dedcd80506c1f531fc76caec8ad504

COPY install_loris.sh /install_loris.sh
RUN /install_loris.sh

# This is an sRGB color profile downloaded from
# http://www.color.org/srgbprofiles.xalter
COPY sRGB2014.icc /usr/share/color/icc/colord/sRGB2014.icc

COPY loris2.wsgi /var/www/loris2/loris2.wsgi
COPY uwsgi.ini /etc/uwsgi
COPY run_loris.sh /

EXPOSE 8888

CMD ["/run_loris.sh"]
