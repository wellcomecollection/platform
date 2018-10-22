#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Example lambda
"""

import json
import os

# Required for elasticsearch-py
import certifi
from elasticsearch import Elasticsearch

# --------------------------------


# Implement transformation here
def _transform(record):
    # do not do this!
    return record


# Implement id extraction here
def _extract_id(doc):
    # do not do this!
    return hash(doc)


# --------------------------------


def extract_records(event):
    return (
        [json.loads(
            record['Sns']['Message']
        ) for record in event['Records']]
    )


def transform_records(records):
    return (
        [_transform(
            record
        ) for record in records]
    )


def _load_record(es, index, doc_type, id, doc):
    return es.index(
        index=index,
        doc_type=doc_type,
        id=id,
        body=doc
    )


def load_records(es, index, doc_type, id, docs):
    return (
        [_load_record(
            es, index, doc_type, id, doc
        ) for doc in docs]
    )


def _run(url, username, password, event, index, doc_type):
    es = Elasticsearch(
        url,
        http_auth=(
            username,
            password
        ),
        use_ssl=True,
        ca_certs=certifi.where()
    )

    records = extract_records(event)
    transformed_records = transform_records(records)

    return load_records(
        es, index, doc_type, id, transformed_records
    )


def main(event, _):
    print(f"Event: {event}")

    results = _run(
        url=os.environ["ES_URL"],
        username=os.environ["ES_USER"],
        password=os.environ["ES_PASS"],
        event=event,
        index=os.environ["ES_INDEX"],
        doc_type=os.environ["ES_DOC_TYPE"]
    )

    print(f"Result: {results}")

