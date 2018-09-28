FROM alpine

RUN apk add --update python3

RUN pip3 install --upgrade pip
RUN pip3 install attrs boto3 pyhcl requests

COPY switch_api_pins.py /
ENTRYPOINT ["python3", "switch_api_pins.py"]
