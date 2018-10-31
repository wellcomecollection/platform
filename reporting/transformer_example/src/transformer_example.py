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


# Moving records through the pipeline ------------------------------------------
def fetch_object_from_s3(s3, hybrid_record):
    """
    fetches an object from s3 based on the data in the given HybridRecord

    Parameters
    ----------
    s3 : boto3.client
    hybrid_record : HybridRecord

    Returns
    -------
    s3_object : dict
    """ 
    return s3.get_object(
        Bucket=hybrid_record.location.namespace, 
        Key=hybrid_record.location.key
    )


def s3_object_to_data_dict(s3_object)
    """ 
    extracts the useful data from a given s3 object. 

    Parameters
    ----------
    s3_object : dict

    Returns
    -------
    data_dict : dict
    """
    record = s3_object["Body"].read().decode("utf-8")
    return json.loads(record)["data"]


def extract_records(s3, event):
    """
    extracts records from VHS and prepares them for transformation

    Parameters
    ----------
    s3 : boto3.client
    event : dict

    Returns
    -------
    data_dicts : dict
    """
    raw_hybrid_records = [
        json.loads(record["Sns"]["Message"]) for record in event["Records"]
    ]
    hybrid_records = [HybridRecord(**record) for record in raw_hybrid_records]
    data_dicts = [
        hybrid_record_to_data_dict(s3, hybrid_record)
        for hybrid_record in hybrid_records
    ]
    return data_dicts, hybrid_records


# Move records with transforms applied -----------------------------------------
def main(event, _, s3_client=None, es_client=None, index=None, doc_type=None):
    """
    get records from VHS, apply the transformation to them, and shove them into
    an elasticsearch index
    """
    s3_client = s3_client or boto3.client("s3")
    index = index or os.environ["ES_INDEX"]
    doc_type = doc_type or os.environ["ES_DOC_TYPE"]
    es_client = es_client or Elasticsearch(
        hosts=os.environ["ES_URL"],
        use_ssl=True,
        ca_certs=certifi.where(),
        http_auth=(os.environ["ES_USER"], os.environ["ES_PASS"]),
    )

    data_dicts, hybrid_records = extract_records(s3_client, event)

    transformed_dicts = [transform(record) for record in data_dicts]

    es_records = [
        ElasticsearchRecord(id=hybrid_record.id, doc=doc)
        for doc, hybrid_record in zip(transformed_dicts, hybrid_records)
    ]

    for record in es_records:
        es_client.index(
            index=index,
            doc_type=doc_type,
            id=record.id,
            body=record.doc
        )
