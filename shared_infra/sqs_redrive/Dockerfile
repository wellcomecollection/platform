FROM python:3-alpine

COPY requirements.txt /
RUN pip install -r requirements.txt

COPY run.py /
CMD ["/run.py"]
