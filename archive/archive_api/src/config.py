# -*- encoding: utf-8

import os

import boto3


class ArchiveAPIConfig(object):

    DYNAMODB_RESOURCE = boto3.resource('dynamodb')
    SNS_CLIENT = boto3.client('sns')

    def __init__(self, development=False):
        try:
            if development:
                self.DYNAMODB_TABLE_NAME = 'archive-storage-progress-table'
                self.SNS_TOPIC_ARN = 'arn:aws:sns:eu-west-1:760097843905:archive-storage_archivist'
            else:
                self.DYNAMODB_TABLE_NAME = os.environ['TABLE_NAME']
                self.SNS_TOPIC_ARN = os.environ['TOPIC_ARN']
        except KeyError as err:
            raise RuntimeError(
                f'Unable to create config: {err!r}'
            )
