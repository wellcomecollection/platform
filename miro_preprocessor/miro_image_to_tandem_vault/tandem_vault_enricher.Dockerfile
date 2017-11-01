FROM python:3-alpine3.6
LABEL maintainer "Wellcome digital platform <wellcomedigitalplatform@wellcome.ac.uk>"
LABEL description "Upload Miro images in Tandem Vault"

RUN apk update

# Required for lxml
RUN apk add build-base libxml2-dev libxslt-dev python3-dev

COPY src/requirements.txt /requirements.txt
RUN pip3 install -r /requirements.txt

COPY src /app

ENTRYPOINT ["/app/tandem_vault_enricher.py"]