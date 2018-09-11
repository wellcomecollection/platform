FROM alpine

RUN apk add --update python3
RUN pip3 install --upgrade pip

COPY requirements.txt /requirements.txt
RUN pip3 install -r /requirements.txt

COPY src /src
WORKDIR /src

EXPOSE 5000

ENTRYPOINT ["python3", "main.py"]
