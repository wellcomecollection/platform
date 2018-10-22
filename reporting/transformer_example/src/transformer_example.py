#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Example lambda
"""

import json
import os

from attr import attrs, attrib
import boto3
# Required for elasticsearch-py
import certifi
from elasticsearch import Elasticsearch


# --------------------------------


# Implement transformation here
def _transform(record):
    # do not do this!
    return record

# --------------------------------


@attrs
class ObjectLocation(object):
    namespace = attrib()
    key = attrib()


@attrs
class HybridRecord(object):
    id = attrib()
    location = attrib()


@attrs
class ElasticsearchRecord(object):
    id = attrib()
    doc = attrib()


def _extract_hybrid_record(raw_record):
    location = ObjectLocation(**raw_record["location"])
    raw_record["location"] = location

    return HybridRecord(**raw_record)


def _get_record_from_s3(s3, object_location):
    obj = s3.get_object(
        Bucket=object_location.namespace,
        Key=object_location.key
    )

    return obj['Body'].read().decode('utf-8')


def _build_es_record(s3, hybrid_record):
    s3_record = _get_record_from_s3(
        s3, hybrid_record.location
    )

    return ElasticsearchRecord(
        id=hybrid_record.id,
        doc=s3_record
    )


def extract_records(s3, event):
    raw_hybrid_records = (
        [json.loads(
            record['Sns']['Message']
        ) for record in event['Records']]
    )

    hybrid_records = (
        [_extract_hybrid_record(
            raw_record
        ) for raw_record in raw_hybrid_records]
    )

    return (
        [_build_es_record(
            s3, hybrid_record
        ) for hybrid_record in hybrid_records]
    )


def transform_records(records):
    return (
        [_transform(
            record
        ) for record in records]
    )


def _load_record(es, index, doc_type, es_record):
    return es.index(
        index=index,
        doc_type=doc_type,
        id=es_record.id,
        body=es_record.doc
    )


def load_records(es, index, doc_type, docs):
    return (
        [_load_record(
            es, index, doc_type, doc
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

    s3 = boto3.client('s3')

    records = extract_records(s3, event)
    transformed_records = transform_records(records)

    return load_records(
        es, index, doc_type, transformed_records
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
