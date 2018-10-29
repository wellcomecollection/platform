#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Config
"""

import os

from models import ElasticsearchConfig


config = ElasticsearchConfig(
    url=os.environ["ES_URL"],
    username=os.environ["ES_USER"],
    password=os.environ["ES_PASS"],
    index=os.environ["ES_INDEX"],
    doc_type=os.environ["ES_DOC_TYPE"]
)