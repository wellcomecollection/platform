# -*- encoding: utf-8 -*-

import pytest
import boto3
import os
import dynamo_write_heartbeat

TABLE_NAMES = 'Test1, Test2'
ENDPOINT_URL = 'http://localhost:8000'


@pytest.yield_fixture(autouse=True)
def run_around_tests():
    dynamodb = boto3.client('dynamodb', endpoint_url=ENDPOINT_URL)
    table_names = [t.strip(' ') for t in TABLE_NAMES.split(",")]
    for table_name in table_names:
        create_table(dynamodb, table_name)
    yield
    for table_name in table_names:
        dynamodb.delete_table(TableName=table_name)


def test_executing_lambda():
    os.environ.update({'TABLE_NAMES': 'Test1, Test2'})
    dynamo_write_heartbeat.main(event=None, context=None, endpoint_url=ENDPOINT_URL)


def create_table(dynamodb, table_name):
    try:
        dynamodb.create_table(TableName=table_name,
                              KeySchema=[
                                  {
                                      'AttributeName': 'id',
                                      'KeyType': 'HASH'
                                  }
                              ],
                              AttributeDefinitions=[
                                  {
                                      'AttributeName': 'id',
                                      'AttributeType': 'S'
                                  }

                              ],
                              ProvisionedThroughput={
                                  'ReadCapacityUnits': 1,
                                  'WriteCapacityUnits': 1
                              })
        dynamodb.get_waiter('table_exists').wait(TableName=table_name)
    except dynamodb.exceptions.ResourceInUseException:
        pass