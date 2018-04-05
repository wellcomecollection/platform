# -*- encoding: utf-8

import run_elasticdump
import os


def test_end_to_end(
    sqs_client,
    queue_url,
    s3_client,
    bucket,
    elasticsearch_index
):
    sqs_client.send_message(QueueUrl=queue_url, MessageBody="")

    os.environ['sqs_queue_url'] = s3_client
    os.environ['upload_bucket'] = bucket
    run_elasticdump.run(sqs_client, s3_client)
