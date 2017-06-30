FROM wellcome/loris-uwsgi

EXPOSE 8888

RUN apk update && apk add python3 && pip3 install awscli && rm -rf /var/cache/apk

COPY run.sh /run.sh
CMD ["/run.sh"]
