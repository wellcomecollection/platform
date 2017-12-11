FROM wellcome/sbt_wrapper

RUN apk update && \
    apk add git python3
RUN pip3 install docopt

COPY . /builds

ENTRYPOINT ["python3", "/builds/build_sbt_image.py"]
