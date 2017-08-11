FROM alpine:edge

RUN apk update
RUN apk add python3

COPY requirements.txt /requirements.txt
RUN pip3 install --requirement /requirements.txt

COPY elasticsearch_cleaner.py /elasticsearch_cleaner.py

CMD ["python3", "/elasticsearch_cleaner.py"]
