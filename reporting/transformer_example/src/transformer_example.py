#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Example lambda
"""

import os

from vhs_etl import ElasticsearchConfig, etl


def transform(doc):
    return doc


def main(event, _):
    print(f"Event: {event}")

    config = ElasticsearchConfig(
        url=os.environ["ES_URL"],
        username=os.environ["ES_USER"],
        password=os.environ["ES_PASS"],
        index=os.environ["ES_INDEX"],
        doc_type=os.environ["ES_DOC_TYPE"]
    )

    results = etl(config, transform, event)

    print(f"Result: {results}")
