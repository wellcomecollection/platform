FROM alpine

RUN apk add --update python3 uwsgi uwsgi-python3
RUN pip3 install --upgrade pip

COPY requirements.txt /requirements.txt
RUN pip3 install -r /requirements.txt

COPY src /src
WORKDIR /src

COPY archive_api.wsgi /var/www/archive_api/archive_api.wsgi
COPY uwsgi.ini /etc/uwsgi

EXPOSE 9000

CMD ["/usr/sbin/uwsgi", "--ini", "/etc/uwsgi/uwsgi.ini"]
