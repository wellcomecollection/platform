#!/usr/bin/env python
# -*- encoding: utf-8

import os

from travistooling import ROOT


def get_rfc_readmes(repo):
    rfcs_dir = os.path.join(repo, 'docs', 'rfcs')
    for root, _, filenames in os.walk(rfcs_dir):
        for f in filenames:
            if f == 'README.md':
                yield os.path.join(root, f)


for f in get_rfc_readmes(ROOT):
    print(f)
