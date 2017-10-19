FROM sierra_adapter:latest

RUN pip3 install pytest

WORKDIR /app
ENTRYPOINT ["py.test", "--verbose"]
