FROM python:3-alpine

COPY requirements.txt /
RUN pip install -r requirements.txt

COPY freezeray.py /
CMD ["sh", "-c", "python3 /freezeray.py --src=${QUEUE_URL} --bucket=${BUCKET}"]
