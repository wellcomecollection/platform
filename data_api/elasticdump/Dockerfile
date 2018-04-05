FROM alpine

RUN apk update && apk add nodejs python3
RUN npm install -g elasticdump

RUN pip3 install --upgrade pip

COPY src/requirements.txt /
RUN pip3 install -r /requirements.txt

ENV PYTHONUNBUFFERED=x

COPY service_utils.py /
COPY run_elasticdump.py /
CMD ["python3", "/run_elasticdump.py"]
