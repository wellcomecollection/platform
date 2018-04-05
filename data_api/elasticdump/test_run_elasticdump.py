# -*- encoding: utf-8

import run_elasticdump
import os
import subprocess


def test_end_to_end(
    sqs_client,
    queue_url,
    s3_client,
    bucket,
    # elasticsearch_index,
    # elasticsearch_hostname
):
    subprocess.check_call([
        'docker', 'run', '--net', 'host',
        '--env', f'sqs_queue_url={queue_url}',
        '--env', f'upload_bucket={bucket}',
        # '--env', f'es_index={elasticsearch_index}',
        # '--env', f'es_username=elastic',
        # '--env', f'es_password=changeme',
        # '--env', f'es_hostname={elasticsearch_hostname}',
        'elasticdump'
    ])
