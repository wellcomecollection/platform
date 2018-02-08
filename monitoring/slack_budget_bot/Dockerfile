FROM python:3

COPY src/requirements.txt /requirements.txt
RUN pip3 install -r /requirements.txt

COPY src /app

CMD ["/app/slack_budget_bot.py"]
