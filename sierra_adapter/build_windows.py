#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Usage: build_windows.py --start=<START> --end=<END> [--interval=<INTERVAL>] --resource=<RESOURCE>
       build_windows.py -h | --help
"""

import datetime as dt
import json
import math

import boto3
import docopt
import maya
import tqdm

args = docopt.docopt(__doc__)

start = maya.parse(args['--start']).datetime()
end = maya.parse(args['--end']).datetime()
minutes = int(args['--interval'] or 30)
resource = args['--resource']

assert resource in ('bibs', 'items')


def generate_windows(start, end, minutes):
    current = start
    while current <= end:
        yield {
            'start': current.isoformat(),
            'end': (current + dt.timedelta(minutes=minutes)).isoformat(),
        }
        current += dt.timedelta(minutes=minutes - 1)


client = boto3.client('sns')

for window in tqdm.tqdm(
    generate_windows(start, end, minutes),
    total=math.ceil((end - start).total_seconds() / 60 / (minutes - 1))
):
    client.publish(
        TopicArn=f'arn:aws:sns:eu-west-1:760097843905:sierra_{resource}_windows',
        Message=json.dumps(window),
        Subject=f'Window sent by {__file__}'
    )
