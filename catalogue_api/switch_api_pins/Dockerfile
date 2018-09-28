FROM alpine

RUN apk add --update docker git make openssh python python3

RUN pip3 install --upgrade pip
RUN pip3 install attrs boto3 pyhcl requests

VOLUME ["/repo"]
WORKDIR /repo

COPY switch_api_pins.py /
ENTRYPOINT ["python3", "/switch_api_pins.py"]
