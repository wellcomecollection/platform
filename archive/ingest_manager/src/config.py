# -*- encoding: utf-8

import os

import boto3


dynamodb_resource = boto3.resource('dynamodb')
table_name = os.environ['TABLE_NAME']
