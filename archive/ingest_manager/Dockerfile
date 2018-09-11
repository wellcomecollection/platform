FROM alpine

RUN apk add --update python3 uwsgi uwsgi-python3
RUN pip3 install --upgrade pip

COPY src/requirements.txt /requirements.txt
RUN pip3 install -r /requirements.txt

COPY src /src
WORKDIR /src

COPY ingest_manager.wsgi /var/www/ingest_manager/ingest_manager.wsgi
COPY uwsgi.ini /etc/uwsgi

EXPOSE 8888

CMD ["/usr/sbin/uwsgi", "--ini", "/etc/uwsgi/uwsgi.ini"]"]
