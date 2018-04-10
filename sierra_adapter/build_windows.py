#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Build 'update windows' for the Sierra adapter pipeline.

Usage:
    build_windows.py --start=<START> --end=<END> [--window_length=<WINDOW_LENGTH>] --resource=<RESOURCE>
    build_windows.py -h | --help

Options:
    --start=<START>         When to start polling
    --end=<END>             The earliest point to stop polling
    --window_length=<WINDOW_LENGTH>
                            The number of minutes per window
    --resource=<RESOURCE>   Should the pipeline fetch bibs or items?


This script generates windows to poll for updates from Sierra, and sends
them to the SNS topic that triggers our pipeline.  You pass it an interval
(start, end), and then it generates windows of length ``window_length`` that
cover the interval.

For example, calling the script with arguments

    start           = 10:00
    end             = 10:59
    window_length   = 15
    resource        = bibs

which generate four windows:

    ( 10:00 ------------------------------------------------------ 11:00 )
    | 10:00 -- 10:15 |
                     | 10:15 -- 10:30 |
                                      | 10:30 -- 10:45 |
                                                       | 10:45 --- 11:00 |

"""

import datetime as dt
import json
import math

import boto3
import docopt
import maya
import pytz
import tqdm


def generate_windows(start, end, minutes):
    current = pytz.utc.localize(start)
    end = pytz.utc.localize(end)
    while current <= end:
        yield {
            'start': current.isoformat(),
            'end': (current + dt.timedelta(minutes=minutes)).isoformat(),
        }
        current += dt.timedelta(minutes=minutes - 1)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    start = maya.parse(args['--start']).datetime()
    end = maya.parse(args['--end']).datetime()
    minutes = int(args['--window_length'] or 30)
    resource = args['--resource']

    assert resource in ('bibs', 'items')

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
