FROM alpine

LABEL maintainer "a.chan@wellcome.ac.uk"
LABEL description="An ECS dashboard for the Wellcome Digital Platform"

# Install the boto3
RUN apk update
RUN apk add python3
RUN pip3 install --upgrade pip

COPY requirements.txt /requirements.txt
RUN pip3 install -r /requirements.txt

COPY dashboard.py /dashboard.py

CMD ["python3", "/dashboard.py"]
