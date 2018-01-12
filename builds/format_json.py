#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json
import os
import subprocess
import sys


def find_json_files():
    for root, _, filenames in os.walk('.'):
        if any(
            d in root
            for d in ['/WIP', '/.terraform', '/target']
        ):
            continue

        for f in filenames:



            if f.lower().endswith('.json'):
                yield os.path.join(root, f)


if __name__ == '__main__':

    bad_files = []

    for f in find_json_files():
        f_contents = open(f).read()
        try:
            data = json.loads(f_contents)
        except ValueError as err:
            print(f'[ERROR] {f} - Invalid JSON? {err}')
            bad_files.append(f)
            continue

        json_str = json.dumps(f_contents, indent=2, sort_keys=True)
        if json_str == f_contents:
            print(f'[OK]    {f}')
        else:
            open(f, 'w').write(json_str)
            print(f'[FIXED] {f}')

    if bad_files:
        print('')
        print('Errors in the following files:')
        for f in bad_files:
            print(f'- {f}')

        sys.exit(1)
    else:
        sys.exit(0)
