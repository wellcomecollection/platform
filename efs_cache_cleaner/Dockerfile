FROM python:3.6-alpine3.6

RUN pip3 install docopt

COPY cache_cleaner.py /.
COPY os_utils.py /.
COPY run.sh /.

VOLUME /data

CMD ["/run.sh"]