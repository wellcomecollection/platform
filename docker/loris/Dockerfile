FROM ubuntu:14.04

RUN apt-get update

RUN apt-get install -y python python-pip python-setuptools python-dev
RUN pip install awscli

RUN apt-get install -y libjpeg-turbo8-dev libfreetype6-dev zlib1g-dev \
    liblcms2-dev liblcms2-utils libtiff5-dev python-dev libwebp-dev apache2 \
    libapache2-mod-wsgi

RUN apt-get install -y unzip wget
RUN wget https://github.com/loris-imageserver/loris/archive/v2.1.0-final.zip
RUN unzip v2.1.0-final.zip

RUN useradd -d /var/www/loris -s /sbin/false loris

RUN cd loris-2.1.0-final && pip install -r requirements.txt
RUN cd loris-2.1.0-final && python setup.py install

RUN apt-get install -y uwsgi

COPY loris2.wsgi /var/www/loris2/loris2.wsgi
COPY uwsgi.ini /etc/uwsgi
COPY uwsgi.ini /etc/uwsgi
COPY run.sh /run.sh

EXPOSE 8888

CMD ["/run.sh"]
