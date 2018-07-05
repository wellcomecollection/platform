# -*- encoding: utf-8 -*-

import pytest
import os
import dynamo_write_heartbeat
import json

HEARTBEAT_CONFIG_KEY = "HEARTBEAT_CONFIG"

TABLES = [{'name': 'Table1', 'index': 'Index1'},
          {'name': 'Table2', 'index': 'Index2'},
          {'name': 'Table3', 'index': 'Index3'}]


@pytest.yield_fixture(autouse=True)
def run_around_tests(dynamodb_client):
    tables = TABLES

    for table in tables:
        create_table(dynamodb_client, table['name'], table['index'])
    yield
    for table in tables:
        dynamodb_client.delete_table(TableName=table['name'])


def test_executing_lambda(dynamodb_client):
    heartbeats_json_str = json.dumps([
        {
            '__heartbeat__': True,
            'table_name': 'Table1',
            'key': {'id': {'S': 'heartbeat-dummy-record-id-1'}},
            'update_expression': "SET indexedId = :v",
            'expression_attribute_values': {':v': {'S': 'value-1'}}
        },
        {
            '__heartbeat__': True,
            'table_name': 'Table3',
            'key': {'id': {'S': 'heartbeat-dummy-record-id-3'}},
            'update_expression': "SET indexedId = :v",
            'expression_attribute_values': {':v': {'S': 'value-3'}}
        }
    ])
    os.environ.update({HEARTBEAT_CONFIG_KEY: heartbeats_json_str})
    dynamo_write_heartbeat.main(event=None, context=None, dynamodb_client=dynamodb_client)

    assert_heartbeat_record_in_table(dynamodb_client,
                                     table_name='Table1',
                                     key={'id': {'S': 'heartbeat-dummy-record-id-1'}},
                                     value={'indexedId': {'S': 'value-1'}})

    table = dynamodb_client.describe_table(TableName='Table2')
    assert table['Table']['ItemCount'] == 0

    assert_heartbeat_record_in_table(dynamodb_client,
                                     table_name='Table3',
                                     key={'id': {'S': 'heartbeat-dummy-record-id-3'}},
                                     value={'indexedId': {'S': 'value-3'}})



def test_executing_lambda_config_not_provided(dynamodb_client):
    del os.environ[HEARTBEAT_CONFIG_KEY]
    with pytest.raises(RuntimeError) as e:
        dynamo_write_heartbeat.main(event=None, context=None, dynamodb_client=dynamodb_client)
    e.match("HEARTBEAT_CONFIG not set")


def test_executing_lambda_config_not_json(dynamodb_client):
    os.environ.update({HEARTBEAT_CONFIG_KEY: 'not-json'})
    with pytest.raises(RuntimeError) as e:
        dynamo_write_heartbeat.main(event=None, context=None, dynamodb_client=dynamodb_client)
    e.match("config 'not-json' could not be parsed")


def test_executing_lambda_config_incomplete(dynamodb_client):
    heartbeats_json_str = json.dumps([
        {
            '__heartbeat__': True,
            'table_name': 'Test1'
        }
    ])
    os.environ.update({HEARTBEAT_CONFIG_KEY: heartbeats_json_str})
    with pytest.raises(RuntimeError) as e:
        dynamo_write_heartbeat.main(event=None, context=None, dynamodb_client=dynamodb_client)
    e.match("Incomplete HeartbeatConfig {'__heartbeat__': True, 'table_name': 'Test1'}")


def create_table(dynamodb_client, table_name, index_name):
    try:
        key_schema = [
            {
                'AttributeName': 'id',
                'KeyType': 'HASH'
            }
        ]
        attribute_definiutions = [
            {
                'AttributeName': 'id',
                'AttributeType': 'S'
            },
            {
                'AttributeName': 'indexedId',
                'AttributeType': 'S'
            }
        ]
        provisioned_throughput = {
            'ReadCapacityUnits': 1,
            'WriteCapacityUnits': 1
        }
        gsi = [{
            'IndexName': index_name,
            'KeySchema': [
                {
                    'AttributeName': 'indexedId',
                    'KeyType': 'HASH'
                }
            ],
            'Projection': {
                'ProjectionType': 'ALL'
            },
            'ProvisionedThroughput': {
                'ReadCapacityUnits': 1,
                'WriteCapacityUnits': 1
            }
        }]
        dynamodb_client.create_table(TableName=table_name,
                                     KeySchema=key_schema,
                                     AttributeDefinitions=attribute_definiutions,
                                     GlobalSecondaryIndexes=gsi,
                                     ProvisionedThroughput=provisioned_throughput)
        dynamodb_client.get_waiter('table_exists').wait(TableName=table_name)
    except dynamodb_client.exceptions.ResourceInUseException:
        pass


def assert_heartbeat_record_in_table(dynamodb_client, table_name, key, value):
    table = dynamodb_client.describe_table(TableName=table_name)
    assert table['Table']['ItemCount'] == 1
    saved = dynamodb_client.get_item(TableName=table_name,
                                     Key=key)
    assert saved['Item'] == {**key, **value}
