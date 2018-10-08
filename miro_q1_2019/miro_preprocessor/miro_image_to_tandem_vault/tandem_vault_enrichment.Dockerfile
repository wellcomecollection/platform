FROM python:3-alpine3.6
LABEL maintainer "Wellcome digital platform <wellcomedigitalplatform@wellcome.ac.uk>"
LABEL description "Enrich Miro images in Tandem Vault"

COPY src/requirements.txt /requirements.txt
RUN pip3 install -r /requirements.txt

COPY src /app

ENTRYPOINT ["/app/tandem_vault_enrichment.py"]