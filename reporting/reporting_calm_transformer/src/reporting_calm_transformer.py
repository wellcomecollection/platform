"""
Basic transformer, which cleans up the static calm data before sending it off
to an elasticsearch index.

The raw data can be obtained by running:

    python monitoring/scripts/download_oai_harvest.py

from the root of this repo. This will create a file called `calm_records.json`.

The elasticsearch credentials (url, username, and password) should be specified
by a `es_credentials.json` file in this directory.
"""
import os
import json
import subprocess
from tqdm import tqdm
from transform import transform
from elasticsearch import Elasticsearch


path_to_es_credentials = os.path.join(
    os.path.dirname(os.path.realpath(__file__)), "es_credentials.json"
)

es_credentials = json.load(open(path_to_es_credentials))

es = Elasticsearch(
    es_credentials["url"],
    http_auth=(es_credentials["username"], es_credentials["password"]),
)

path_to_raw_records = (
    subprocess.check_output(["git", "rev-parse", "--show-toplevel"])
    .strip()
    .decode("utf8")
    + "/calm_records.json"
)

raw_records = json.load(open(path_to_raw_records))

for raw_record in tqdm(raw_records):
    try:
        record = transform(raw_record)
        res = es.index(
            index="calm", id=record["RecordID"], doc_type="calm_record", body=record
        )
    except Exception as e:
        print(e)
