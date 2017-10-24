#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json

import boto3

sqs_client = boto3.client('sqs')

messages = []

resp = sqs_client.receive_message(
    QueueUrl='https://sqs.eu-west-1.amazonaws.com/760097843905/alex-test-queue',
    MaxNumberOfMessages=10,
)

messages.extend([json.dumps(m).encode('ascii') for m in resp['Messages']])

s3_client = boto3.client('s3')

data = b'\n'.join(messages)
s3_client.put_object(
    Bucket='platform-infra',
    Key='sqs/alex-test-queue.txt',
    Body=data
)

from pprint import pprint
print(data)