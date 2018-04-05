# -*- encoding: utf-8

import json
import os
import subprocess
import time

import requests


def test_end_to_end(
    sqs_client,
    queue_url,
    s3_client,
    bucket,
    sqs_endpoint_url,
    s3_endpoint_url,
    elasticsearch_index,
    elasticsearch_url,
    elasticsearch_hostname
):
    sqs_client.send_message(
        QueueUrl=queue_url,
        MessageBody=json.dumps({
            'target_bucket': bucket,
            'target_key': 'dump.txt.gz',
        })
    )

    for i in range(10):
        resp = requests.put(
            f'{elasticsearch_url}/{elasticsearch_index}/doc/{i}',
            data=json.dumps({
                'user': f'user_{i}',
            }),
            auth=('elastic', 'changeme')
        )
        resp.raise_for_status()

    resp = requests.get(
        f'{elasticsearch_url}/{elasticsearch_index}',
        auth=('elastic', 'changeme')
    )
    from pprint import pprint
    pprint(resp.json())

    subprocess.check_call([
        'docker', 'run', '--net', 'host',
        '--env', f'sqs_queue_url={queue_url}',
        '--env', f'upload_bucket={bucket}',

        '--env', 'AWS_DEFAULT_REGION=localhost',
        '--env', 'AWS_ACCESS_KEY_ID=accessKey1',
        '--env', 'AWS_SECRET_ACCESS_KEY=verySecretKey1',

        '--env', f'local_s3_endpoint={s3_endpoint_url}',
        '--env', f'local_sqs_endpoint={sqs_endpoint_url}',
        '--env', f'es_index={elasticsearch_index}',
        '--env', f'es_username=elastic',
        '--env', f'es_password=changeme',
        '--env', f'es_hostname={elasticsearch_hostname}',
        '--env', 'es_port=9200',
        '--env', 'es_scheme=http://',
        'elasticdump'
    ])

    for _ in range(60):
        try:
            obj = s3_client.get_object(
                Bucket=target_bucket,
                Key='dump.txt.gz'
            )
            body = obj['Body'].read()
            assert body == ''


        except Exception:
            time.sleep(1)
            continue
        else:
            break

    else:  # no break
        assert False, "Never completed successfully"
