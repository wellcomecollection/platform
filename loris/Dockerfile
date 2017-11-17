FROM ubuntu:16.04

RUN apt-get update && \
    apt-get install --yes python uwsgi uwsgi-plugin-python wget

# Install pip.  We don't use pip from the Ubuntu package repositories
# because it tends to be out-of-date and using it gets issues like:
# https://github.com/pyca/cryptography/issues/3959
RUN wget https://bootstrap.pypa.io/get-pip.py
RUN python ./get-pip.py

RUN pip install awscli

ENV LORIS_COMMIT e72d9bf00bd0a4f7052765af6e6ce5480cca5ef3
ENV LORIS_GITHUB_USER loris-imageserver

COPY requirements.txt /
COPY install_loris.sh /install_loris.sh
RUN /install_loris.sh

# This is an sRGB color profile downloaded from
# http://www.color.org/srgbprofiles.xalter
COPY sRGB2014.icc /usr/share/color/icc/colord/sRGB2014.icc

COPY loris2.wsgi /var/www/loris2/loris2.wsgi
COPY uwsgi.ini /etc/uwsgi
COPY run_loris.sh /

ENV LORIS_CONF_FILE /opt/loris/etc/loris2.conf

EXPOSE 8888

CMD ["/run_loris.sh"]
