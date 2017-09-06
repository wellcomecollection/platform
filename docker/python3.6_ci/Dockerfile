FROM python:3.6

VOLUME ["/data"]

RUN pip install flake8

RUN pip install boto3

RUN pip install pytest

RUN pip install moto==1.0.1

COPY is_up_to_date_with_master.py /app/is_up_to_date_with_master.py

COPY run.sh /app/run.sh

WORKDIR /data

ENTRYPOINT /app/run.sh
