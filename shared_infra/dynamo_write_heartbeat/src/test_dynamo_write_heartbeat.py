# -*- encoding: utf-8 -*-

import pytest
import os
import dynamo_write_heartbeat


TABLE_NAMES = ["Test1", "Test2"]
TABLE_NAME_STR = ",".join(TABLE_NAMES)


@pytest.yield_fixture(autouse=True)
def run_around_tests(dynamodb_client):
    table_names = TABLE_NAMES

    for table_name in table_names:
        create_table(dynamodb_client, table_name)
    yield
    for table_name in table_names:
        dynamodb_client.delete_table(TableName=table_name)


def test_executing_lambda_no_params(dynamodb_client):
    with pytest.raises(RuntimeError) as e:
        dynamo_write_heartbeat.main(
            event=None, context=None, dynamodb_client=dynamodb_client
        )
    e.match("TABLE_NAMES not found")


def test_executing_lambda(dynamodb_client):
    os.environ.update({"TABLE_NAMES": TABLE_NAME_STR})
    dynamo_write_heartbeat.main(
        event=None, context=None, dynamodb_client=dynamodb_client
    )


def test_lambda_does_not_error_if_table_does_not_exist(dynamodb_client):
    os.environ.update({"TABLE_NAMES": ",".join(TABLE_NAMES + ["DoesNotExist"])})
    dynamo_write_heartbeat.main(dynamodb_client=dynamodb_client)


def create_table(dynamodb, table_name):
    try:
        dynamodb.create_table(
            TableName=table_name,
            KeySchema=[{"AttributeName": "id", "KeyType": "HASH"}],
            AttributeDefinitions=[{"AttributeName": "id", "AttributeType": "S"}],
            ProvisionedThroughput={"ReadCapacityUnits": 1, "WriteCapacityUnits": 1},
        )
        dynamodb.get_waiter("table_exists").wait(TableName=table_name)
    except dynamodb.exceptions.ResourceInUseException:
        pass
