#!/usr/bin/env python
# -*- encoding: utf-8
"""
Report the progress of the Sierra adapter.

Usage:
    report_adapter_progress.py [--purge_dlqs]
    report_adapter_progress.py -h | --help

Options:
    --purge_dlqs    Purge the associated DLQs *if* the report shows that
                    the windows don't have any gaps.
    -h --help       Show this message.

"""

import datetime as dt
import os
import sys

import attr
import boto3
import docopt


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

        try:
            contents = resp['Contents']
        except KeyError:
            contents = []

        for obj in contents:
            yield obj['Key']

        # The S3 API is paginated, returning up to 1000 keys at a time.
        # Pass the continuation token into the next response, until we
        # reach the final page (when this field is missing).
        try:
            kwargs['ContinuationToken'] = resp['NextContinuationToken']
        except KeyError:
            break


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
    args = docopt.docopt(__doc__)

    final_reports = {}

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

        # Now we've consolidated the markers in S3, produce a second copy
        # of the report that "summarises" the output.
        report = build_report(bucket=BUCKET, resource_type=resource_type)
        final_reports[resource_type] = [iv for (iv, _) in report]

    # We only need to give instructions for building missing windows if
    # there are gaps in the run.
    #
    if len(final_reports['bibs']) > 1 or len(final_reports['items']) > 1:
        print('')
        print('-' * 79)
        print('')
        print('You can build new windows for the gaps:')
        print('')
        print(f'$ python {sys.argv[0].replace("report_adapter_progress.py", "build_missing_windows.py")}')\

    # If the run completed successfully, we can purge the associated DLQs.
    #
    # Because each timestamp is fetched (at least) twice, we can have
    # windows land on the DLQ and still get a complete history from Sierra --
    # every record in that window was picked up as part of a different window.
    #
    if args['--purge_dlqs']:
        print('Purging DLQs...')
        for resource_type in ('bibs', 'items'):
            if len(final_reports[resource_type]) == 1:
                sqs = boto3.client('sqs')
                queue_url = sqs.get_queue_url(
                    QueueName=f'sierra_{resource_type}_windows_dlq'
                )['QueueUrl']
                sqs.purge_queue(QueueUrl=queue_url)
