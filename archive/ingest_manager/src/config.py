# -*- encoding: utf-8

import os

import boto3


dynamodb_resource = boto3.resource('dynamodb')
dynamodb_table_name = os.environ['TABLE_NAME']

sns_client = boto3.client('sns')
sns_topic_arn = os.environ['TOPIC_ARN']
