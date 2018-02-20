#!/usr/bin/env python
# -*- encoding: utf-8
"""
Report the progress of the Sierra adapter.
"""

import datetime as dt
import os

import attr
import boto3


BUCKET = 'wellcomecollection-platform-adapters-sierra'


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


@attr.s
class Interval:
    start = attr.ib()
    end = attr.ib()
    key = attr.ib()


def get_intervals(keys):
    """
    Generate the intervals completed for a particular resource type.

    :param keys: A generator of S3 key names.

    """
    for k in keys:
        name = os.path.basename(k)
        start, end = name.split('__')
        start = start.strip('Z')
        end = end.strip('Z')
        try:
            yield Interval(
                start=dt.datetime.strptime(start, '%Y-%m-%dT%H-%M-%S.%f+00-00'),
                end=dt.datetime.strptime(end, '%Y-%m-%dT%H-%M-%S.%f+00-00'),
                key=k
            )
        except ValueError:
            yield Interval(
                start=dt.datetime.strptime(start, '%Y-%m-%dT%H-%M-%S+00-00'),
                end=dt.datetime.strptime(end, '%Y-%m-%dT%H-%M-%S+00-00'),
                key=k
            )


def combine_overlapping_intervals(sorted_intervals):
    """
    Given a generator of sorted open intervals, generate the covering set.
    It produces a series of 2-tuples: (interval, running), where ``running``
    is the set of sub-intervals used to build the overall interval.

    :param sorted_intervals: A generator of ``Interval`` instances.

    """
    lower = None
    running = []

    for higher in sorted_intervals:
        if not lower:
            lower = higher
            running.append(higher)
        else:
            # We treat these as open intervals.  This first case is for the
            # two intervals being wholly overlapping, for example:
            #
            #       ( -- lower -- )
            #               ( -- higher -- )
            #
            if higher.start < lower.end:
                upper_bound = max(lower.end, higher.end)
                lower = Interval(start=lower.start, end=upper_bound, key=None)
                running.append(higher)

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
                yield (lower, running)
                lower = higher
                running = [higher]

    # And spit out the final interval
    if lower is not None:
        yield (lower, running)


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


def chunks(iterable, chunk_size):
    return (
        iterable[i:i + chunk_size]
        for i in range(0, len(iterable), chunk_size)
    )


if __name__ == '__main__':
    for resource_type in ('bibs', 'items'):
        print('')
        print('=' * 79)
        print(f'{resource_type} windows')
        print('=' * 79)

        report = build_report(bucket=BUCKET, resource_type=resource_type)

        for iv, running in report:
            if len(running) > 1:
                client = boto3.client('s3')

                # Create a consolidated marker that represents the entire
                # interval.  The back-history of Sierra includes >100k windows,
                # so combining them makes reporting faster on subsequent runs.
                start_str = iv.start.strftime("%Y-%m-%dT%H-%M-%S.%f+00-00")
                end_str = iv.end.strftime("%Y-%m-%dT%H-%M-%S.%f+00-00")

                consolidated_key = f'windows_{resource_type}_complete/{start_str}__{end_str}'

                client.put_object(
                    Bucket=BUCKET,
                    Key=consolidated_key,
                    Body=b''
                )

                # Then clean up the individual intervals that made up the set.
                # We sacrifice granularity for performance.
                for sub_ivs in chunks(running, chunk_size=1000):
                    keys = [
                        s.key for s in sub_ivs if s.key != consolidated_key
                    ]
                    client.delete_objects(
                        Bucket=BUCKET,
                        Delete={
                            'Objects': [{'Key': k} for k in keys]
                        }
                    )

            print(f'{iv.start.isoformat()} -- {iv.end.isoformat()}')

        print('')
