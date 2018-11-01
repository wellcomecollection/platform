#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
get records from VHS, apply the transformation to them, and shove them into
an elasticsearch index
"""
from transform import transform
import json
import os

from attr import attrs, attrib
import boto3

# Required for elasticsearch-py
import certifi
from elasticsearch import Elasticsearch


# Classes covering various record types ----------------------------------------
def dict_to_location(d):
    return ObjectLocation(**d)


@attrs
class ObjectLocation(object):
    namespace = attrib()
    key = attrib()


@attrs
class HybridRecord(object):
    id = attrib()
    version = attrib()
    location = attrib(converter=dict_to_location)


@attrs
class ElasticsearchRecord(object):
    id = attrib()
    doc = attrib()


# Move records through the pipeline --------------------------------------------
def extract_sns_messages_from_event(event):
    for record in event["Records"]:
        yield json.loads(record["Sns"]["Message"])


def get_s3_objects_from_messages(s3, messages):
    for message in messages:
        hybrid_record = HybridRecord(**message)
        s3_object = s3.get_object(
            Bucket=hybrid_record.location.namespace, Key=hybrid_record.location.key
        )
        yield hybrid_record.id, s3_object


def unpack_json_from_s3_objects(s3_objects):
    for hybrid_record_id, s3_object in s3_objects:
        record = s3_object["Body"].read().decode("utf-8")
        yield hybrid_record_id, json.loads(record)


def transform_data_for_es(data):
    for hybrid_record_id, data_dict in data:
        yield ElasticsearchRecord(id=hybrid_record_id, doc=transform(data_dict))


# Move records with transforms applied -----------------------------------------
def main(event, _, s3_client=None, es_client=None, index=None, doc_type=None):
    s3 = s3_client or boto3.client("s3")
    index = index or os.environ["ES_INDEX"]
    doc_type = doc_type or os.environ["ES_DOC_TYPE"]
    es_client = es_client or Elasticsearch(
        hosts=os.environ["ES_URL"],
        use_ssl=True,
        ca_certs=certifi.where(),
        http_auth=(os.environ["ES_USER"], os.environ["ES_PASS"]),
    )

    messages = extract_sns_messages_from_event(event)
    s3_objects = get_s3_objects_from_messages(s3, messages)
    data = unpack_json_from_s3_objects(s3_objects)
    es_records_to_send = transform_data_for_es(data)

    for record in es_records_to_send:
        es_client.index(index=index, doc_type=doc_type, id=record.id, body=record.doc)
