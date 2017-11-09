FROM sierra_adapter:latest

RUN pip3 install pytest betamax

WORKDIR /app
ENTRYPOINT ["py.test", "--verbose"]
