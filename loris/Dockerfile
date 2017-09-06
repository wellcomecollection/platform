FROM ubuntu:14.04

RUN apt-get update

RUN apt-get install -y python python-pip python-setuptools python-dev \
    uwsgi-plugin-python

RUN pip install awscli

ENV LORIS_COMMIT 400bc3979a7dc3ff938abff84665c2731f750a69

COPY install_loris.sh /install_loris.sh
RUN /install_loris.sh

RUN apt-get install -y uwsgi

COPY loris2.wsgi /var/www/loris2/loris2.wsgi
COPY uwsgi.ini /etc/uwsgi
COPY run_loris.sh /

EXPOSE 8888

CMD ["/run_loris.sh"]
