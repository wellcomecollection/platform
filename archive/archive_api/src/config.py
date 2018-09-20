# -*- encoding: utf-8

import os

import boto3

basedir = os.path.abspath(os.path.dirname(__file__))


class Config(object):
    pass


class ProductionConfig(Config):
    DYNAMODB_TABLE_NAME = os.environ['TABLE_NAME']
    SNS_TOPIC_ARN = os.environ['TOPIC_ARN']
    DYNAMODB_RESOURCE = boto3.resource('dynamodb')
    SNS_CLIENT = boto3.client('sns')
