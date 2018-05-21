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


print('*** Checking RFC headers')

for f in get_rfc_readmes(ROOT):
    print('*** Checking header for %s' % os.path.relpath(f, start=ROOT))
    filename = os.path.basename(os.path.dirname(f))
    number, name = filename.split('-', 1)

    contents = open(f).read()
    header = contents.splitlines()[:3]

    assert header[0].startswith('# RFC %03d: ' % int(number))
    assert header[1] == ''

    print(f, name)
    print(header)
