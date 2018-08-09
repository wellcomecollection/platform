FROM ubuntu:16.04

# Install base Python and uWSGI (web server) dependencies
RUN apt-get update && \
    apt-get install --yes python3 python3-pip uwsgi uwsgi-plugin-python3 && \
    apt-get clean

RUN pip3 install --upgrade pip
RUN pip install flask flask_restful numpy flask_cors

WORKDIR /app

ADD app /app
ADD data /data

EXPOSE 80

CMD ["python3", "/app/app.py"]
