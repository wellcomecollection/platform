#!/usr/bin/env python
# -*- encoding: utf-8
"""
Report the progress of the Sierra adapter.
"""

import collections
import datetime as dt
import os

import boto3


BUCKET = 'wellcomecollection-sierra-adapter-data'


def get_matching_s3_keys(bucket, prefix=''):
    """
    Generate the keys in an S3 bucket.

    :param bucket: Name of the S3 bucket.
    :param prefix: Only fetch keys that start with this prefix (optional).

    """
    # https://alexwlchan.net/2017/07/listing-s3-keys/
    s3 = boto3.client('s3')
    kwargs = {'Bucket': bucket}

    # If the prefix is a single string (not a tuple of strings), we can
    # do the filtering directly in the S3 API.
    if prefix:
        kwargs['Prefix'] = prefix

    while True:

        # The S3 API response is a large blob of metadata.
        # 'Contents' contains information about the listed objects.
        resp = s3.list_objects_v2(**kwargs)
        for obj in resp['Contents']:
            yield obj['Key']

        # The S3 API is paginated, returning up to 1000 keys at a time.
        # Pass the continuation token into the next response, until we
        # reach the final page (when this field is missing).
        try:
            kwargs['ContinuationToken'] = resp['NextContinuationToken']
        except KeyError:
            break


Interval = collections.namedtuple('Interval', ['start', 'end'])


def get_intervals(keys):
    """
    Generate the intervals completed for a particular resource type.

    :param keys: A generator of S3 key names.

    """
    for k in keys:
        name = os.path.basename(k)
        start, end = name.split('__')
        try:
            yield Interval(
                start=dt.datetime.strptime(start, '%Y-%m-%dT%H-%M-%S.%f+00-00'),
                end=dt.datetime.strptime(end, '%Y-%m-%dT%H-%M-%S.%f+00-00')
            )
        except ValueError:
            yield Interval(
                start=dt.datetime.strptime(start, '%Y-%m-%dT%H-%M-%S+00-00'),
                end=dt.datetime.strptime(end, '%Y-%m-%dT%H-%M-%S+00-00')
            )


def combine_overlapping_intervals(sorted_intervals):
    """
    Given a generator of sorted open intervals, generate the covering set.

    :param sorted_intervals: A generator of ``Interval`` instances.

    """
    lower = None

    for higher in sorted_intervals:
        if not lower:
            lower = higher
        else:
            # We treat these as open intervals.  This first case is for the
            # two intervals being wholly overlapping, for example:
            #
            #       ( -- lower -- )
            #               ( -- higher -- )
            #
            if higher.start < lower.end:
                upper_bound = max(lower.end, higher.end)
                lower = Interval(start=lower.start, end=upper_bound)

            # Otherwise the two intervals are disjoint.  Note that this
            # includes the case where lower.end == higher.start, because
            # we can't be sure that point has been included.
            #
            #       ( -- lower -- )
            #                      ( -- higher -- )
            #
            # or
            #
            #       ( -- lower -- )
            #                           ( -- higher -- )
            #
            else:
                yield lower
                lower = higher

    # And spit out the final interval
    if lower is not None:
        yield lower


def build_report(bucket, resource_type):
    """
    Generate a complete set of covering windows for a resource type.
    """
    keys = get_matching_s3_keys(
        bucket=BUCKET,
        prefix=f'windows_{resource_type}_complete'
    )
    intervals = get_intervals(keys=keys)
    yield from combine_overlapping_intervals(intervals)


if __name__ == '__main__':
    for resource_type in ('bibs', 'items'):
        print('=' * 79)
        print(f'{resource_type} windows')
        print('=' * 79)

        for iv in build_report(bucket=BUCKET, resource_type=resource_type):
            print(f'{iv.start.isoformat()} -- {iv.end.isoformat()}')

        print('')
