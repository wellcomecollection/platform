# -*- encoding: utf-8

import os

import boto3


class ArchiveAPIConfig(object):

    DYNAMODB_RESOURCE = boto3.resource('dynamodb')
    SNS_CLIENT = boto3.client('sns')
    S3_CLIENT = boto3.client('s3')

    def __init__(self, development=False):
        try:
            if development:
                self.DYNAMODB_TABLE_NAME = 'archive-storage-progress-table'
                self.SNS_TOPIC_ARN = 'arn:aws:sns:eu-west-1:760097843905:archive-storage_archivist'
                self.BAG_VHS_BUCKET_NAME = 'wellcomecollection-vhs-archive-manifests'
                self.BAG_VHS_TABLE_NAME = 'vhs-archive-manifests'
            else:
                self.DYNAMODB_TABLE_NAME = os.environ['TABLE_NAME']
                self.SNS_TOPIC_ARN = os.environ['TOPIC_ARN']
                self.BAG_VHS_BUCKET_NAME = os.environ['BAG_VHS_BUCKET_NAME']
                self.BAG_VHS_TABLE_NAME = os.environ['BAG_VHS_TABLE_NAME']
        except KeyError as err:
            raise RuntimeError(f'Unable to create config: {err!r}')
