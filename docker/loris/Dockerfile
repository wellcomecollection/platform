FROM wellcome/loris-uwsgi

EXPOSE 8888

RUN apk update && \
    apk add findutils python3 && \
    pip3 install awscli && \
    rm -rf /var/cache/apk

COPY uwsgi.ini /etc/uwsgi
COPY run.sh /run.sh
CMD ["/run.sh"]
