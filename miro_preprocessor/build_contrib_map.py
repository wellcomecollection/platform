#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Build the miro_contributor_map.json used in the common lib.

This is not intended to be run regularly, just as a one-off script.
"""

import json
import os
import subprocess

import boto3
from lxml import etree


ROOT = subprocess.check_output(
    ['git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()

OUTFILE = os.path.join(
    ROOT, 'common/src/main/resources/miro_contributor_map.json')


client = boto3.client('s3')
contributor_xml_object = client.get_object(
    Bucket='miro-data',
    Key='contributors.xml'
)
contributor_xml = contributor_xml_object['Body'].read()

contributor_codes = {}

contributors = etree.fromstring(contributor_xml)
for c in contributors:
    data = {attr.tag: attr.text for attr in c}
    c_id = data['contributor_id']
    c_name = data['contributor_credit_line']

    # Fix up the credit line so that references to Wellcome Images, etc.
    # all point to Wellcome Collection, and general clean-ups.
    c_name = c_name.replace('Wellcome Images', 'Wellcome Collection')
    c_name = c_name.replace('Wellcome Library, London', 'Wellcome Collection')
    c_name = c_name.replace('Wellcome Photo Library', 'Wellcome Collection')
    c_name = c_name.strip()

    # Check there aren't duplicates
    assert c_id not in contributor_codes

    contributor_codes[c_id] = c_name


# Make some spelling corrections, typo fixes, etc.
changes = {
    'ALN': (
        'Nigel Allinson, Univeristy of Lincoln',
        'Nigel Allinson, University of Lincoln'),
    'BEN': (
        'Abigail Benn, University of Bristol, Wellcome Trust Project grant awarded\n\nto Emma Robinson and Anja Teschemacher',
        'Abigail Benn, University of Bristol, Wellcome Trust Project grant awarded to Emma Robinson and Anja Teschemacher'),
    'CRR': (
        'Amanda Carr &',
        'Amanda Carr'),
    'GRE': (
        'David Gregory&Debbie Marshall',
        'David Gregory & Debbie Marshall'),
    'REV': (
        "'EM Unit, UCL Medical School, Royal Free Campus'",
        'EM Unit, UCL Medical School, Royal Free Campus'),
    'THA': (
        'Dr Thanuja Perera,U.Aberdeen',
        'Dr Thanuja Perera, U.Aberdeen'),

    # We could do a better name of these when we pull out the image metadata,
    # but we don't, so for now, strip the placeholder text.
    'CRG': (
        "[Scientist's name], Centre de Regulació Genòmica",
        'Centre de Regulació Genòmica'),
    'FCI': (
        "[scientist's name], Francis Crick Institute",
        'Francis Crick Institute'),
    'HWU': (
        "[scientist's name], Heriot-Watt University",
        'Heriot-Watt University'),
    'ICI': (
        "[scientist's name], Imperial College London",
        'Imperial College London'),
    'KCL': (
        '[individual scientists name], KCL',
        'KCL'),
    'MIT': (
        "[scientist's name(s)], [institute name if applicable], copyright MIT",
        'MIT'),
    'MNS': (
        "[scientist's name], Monash University",
        'Monash University'),
    'RCI': (
        '[creator name], Royal College of Surgeons in Ireland',
        'Royal College of Surgeons in Ireland'),
    'ULI': (
        "[scientist's name], University of Lincoln",
        'University of Lincoln'),
}

for key, (orig, repl) in changes.items():
    assert contributor_codes[key] == orig
    contributor_codes[key] = repl


# Delete certain entries that we don't have contributor codes for.
# If they error out in the transformer, we can go back and assign an
# explicit override in the dictionary above.
deletions = {
    'CBT': "Scientist's name",
    'CHC': 'various - see images',
    'CRW': 'TBC',
    'CSC': 'use individuals',
    'CWM': 'TBC',
    'DNO': 'to be entered',
    'EDC': 'See individuals',
    'FDN': 'TBC',
    'GGC': 'TBC',
    'HHO': 'TBC',
    'ICY': 'tbc',
    'INC': 'See individual contributor records',
    'JAC': 'TBC',
    'KEL': 'see individual entries',
    'MBB': 'tbc',
    'MDH': 'to be entered',
    'MDN': 'TBC',
    'MKK': 'TBC',
    'MRC': 'the contributing unit',
    'MSK': 'Individual scientist',
    'RAN': 'to be confirmed',
    'RHL': 'TBC',
    'RSM': 'TBC',
    'RWI': 'to follow',
    'SGU': 'individual scientists',
    'SHF': 'individual contributors',
    'STN': 'TBC',
    'WAT': 'Individual Scientists',
    'ZGS': 'TBC',
}

for key, orig in deletions.items():
    assert contributor_codes[key] == orig
    del contributor_codes[key]


json_string = json.dumps(contributor_codes, sort_keys=True, indent=2)
open(OUTFILE, 'w').write(json_string)
