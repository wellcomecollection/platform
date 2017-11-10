FROM sierra_objects_to_s3:latest

RUN pip3 install pytest betamax moto

WORKDIR /app
ENTRYPOINT ["py.test", "--verbose"]
