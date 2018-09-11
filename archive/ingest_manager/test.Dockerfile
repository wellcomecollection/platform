FROM alpine

RUN apk add --update python3
RUN pip3 install --upgrade pip

COPY requirements.txt /requirements.txt
COPY test_requirements.txt /test_requirements.txt
RUN pip3 install -r /requirements.txt -r /test_requirements.txt

WORKDIR /src

ENTRYPOINT ["py.test", "tests"]
