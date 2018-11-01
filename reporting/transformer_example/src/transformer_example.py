#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Example lambda
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


def get_hybrid_objects_from_messages(s3, messages):
    for message in messages:
        hybrid_record = HybridRecord(**message)
        s3_object = s3.get_object(
            Bucket=hybrid_record.location.namespace,
            Key=hybrid_record.location.key
        )
        yield hybrid_record, s3_object


def s3_object_to_data_dict(s3_object):
    record = s3_object["Body"].read().decode("utf-8")
    return json.loads(record)["data"]


def get_data_dicts_from_s3_objects(s3_objects):
    for s3_object in s3_objects:
        data_dict = s3_object_to_data_dict(s3_object)
        yield data_dict


# Move records with transforms applied -----------------------------------------
def main(event, _, s3_client=None, es_client=None, index=None, doc_type=None):
    """
    get records from VHS, apply the transformation to them, and shove them into
    an elasticsearch index
    """
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
    hybrid_records, s3_objects = get_hybrid_objects_from_messages(s3, messages)
    data_dicts = get_data_dicts_from_s3_objects(s3_objects)
    transformed_dicts = [transform(data) for data in data_dicts]

    es_records_to_send = [
        ElasticsearchRecord(id=hybrid_record.id, doc=doc)
        for doc, hybrid_record in zip(transformed_dicts, hybrid_records)
    ]

    for record in es_records_to_send:
        es_client.index(
            index=index,
            doc_type=doc_type,
            id=record.id,
            body=record.doc
        )
