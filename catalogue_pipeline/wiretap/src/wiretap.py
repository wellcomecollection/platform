import json
import os
import uuid

import boto3


def main(event, context=None, s3_client=None):
    s3_client = s3_client or boto3.client('s3')

    bucket = os.environ['BUCKET']

    for record in event['Records']:
        sns_event = record['Sns']
        s3_client.put_object(
            Bucket=bucket,
            Key=str(uuid.uuid4()),
            Body=json.dumps(sns_event, separators=(',',':'))
        )
