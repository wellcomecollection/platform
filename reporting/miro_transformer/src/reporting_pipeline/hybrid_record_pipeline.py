#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
get records from VHS, apply the transformation to them, and shove them into
an elasticsearch index
"""
import os
import json
import boto3
import certifi
from attr import attrs, attrib
from elasticsearch import Elasticsearch
from wellcome_aws_utils.lambda_utils import log_on_error


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


def transform_data_for_es(data, transform):
    for hybrid_record_id, data_dict in data:
        yield ElasticsearchRecord(id=hybrid_record_id, doc=transform(data_dict))


# Move records with transforms applied -----------------------------------------
@log_on_error
def process_messages(
    event, transform, s3_client=None, es_client=None, index=None, doc_type=None
):
    s3_client = s3_client or boto3.client("s3")
    index = index or os.environ["ES_INDEX"]
    doc_type = doc_type or os.environ["ES_DOC_TYPE"]
    es_client = es_client or Elasticsearch(
        hosts=os.environ["ES_URL"],
        use_ssl=True,
        ca_certs=certifi.where(),
        http_auth=(os.environ["ES_USER"], os.environ["ES_PASS"]),
    )

    _process_messages(event, transform, s3_client, es_client, index, doc_type)


def _process_messages(event, transform, s3_client, es_client, index, doc_type):
    messages = extract_sns_messages_from_event(event)
    s3_objects = get_s3_objects_from_messages(s3_client, messages)
    data = unpack_json_from_s3_objects(s3_objects)
    es_records_to_send = transform_data_for_es(data, transform)

    for record in es_records_to_send:
        es_client.index(index=index, doc_type=doc_type, id=record.id, body=record.doc)
