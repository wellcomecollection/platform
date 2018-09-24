# -*- encoding: utf-8

import os

import boto3

basedir = os.path.abspath(os.path.dirname(__file__))


class Config(object):
    pass


class ProductionConfig(Config):
    DYNAMODB_TABLE_NAME = os.environ.get('TABLE_NAME')
    SNS_TOPIC_ARN = os.environ.get('TOPIC_ARN')
    DYNAMODB_RESOURCE = boto3.resource('dynamodb')
    SNS_CLIENT = boto3.client('sns')


class DevelopmentConfig(Config):
    DYNAMODB_TABLE_NAME = 'archive-storage-progress-table'
    SNS_TOPIC_ARN = 'arn:aws:sns:eu-west-1:760097843905:archive-storage_archivist'
    DYNAMODB_RESOURCE = boto3.resource('dynamodb')
    SNS_CLIENT = boto3.client('sns')
