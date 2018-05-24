#!/usr/bin/env python3
# -*- encoding: utf-8
"""
This script has two responsibilities:
1.  Check the reference data is well-formatted
2.  Copy the reference data into the target application

"""

import csv
import os
import shutil
import subprocess

# This Python snippet gives you the directory that the current running script
# lives in -- which happens to be the 'Reference data' directory.
REFERENCE_DATA = os.path.dirname(os.path.abspath(__file__))

# Root of the repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('utf8').strip()


def check_identifier_schemes():
    id_schemes = os.path.join(REFERENCE_DATA, 'identifier-schemes.csv')
    print('*** Checking %s' % id_schemes)

    print('\tIt should be a CSV file with three columns per row.')
    with open(id_schemes) as csvfile:
        csvreader = csv.reader(csvfile)
        for row in csvreader:
            assert len(row) == 3, row

    print('\tIt should be copied to internal_model')
    shutil.copyfile(
        src=id_schemes,
        dst=os.path.join(ROOT, 'sbt_common/internal_model/src/main/resources/identifier-schemes.csv')
    )


if __name__ == '__main__':
    check_identifier_schemes()
