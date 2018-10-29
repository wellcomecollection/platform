#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Example lambda
"""

from vhs_etl import ElasticsearchConfig, etl


def transform(doc):
    return doc


def main(event, _):
    print(f"Event: {event}")



    results = etl(config, transform, event)

    print(f"Result: {results}")
