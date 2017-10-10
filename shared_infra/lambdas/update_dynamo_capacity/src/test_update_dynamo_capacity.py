import datetime
import json
from unittest.mock import MagicMock

import update_dynamo_capacity
from dateutil.tz import tzlocal


def _update_response(desired_capacity):
    return {
        'TableDescription': {
            'AttributeDefinitions': [
                {'AttributeName': 'id', 'AttributeType': 'S'},
                {'AttributeName': 'bu', 'AttributeType': 'S'}],
            'TableName': 'TestTable',
            'KeySchema': [{'AttributeName': 'id', 'KeyType': 'HASH'}],
            'TableStatus': 'ACTIVE',
            'CreationDateTime':
                datetime.datetime(2017, 7, 17, 14, 58, 32, 171527,
                                  tzinfo=tzlocal()),
            'ProvisionedThroughput': {
                'ReadCapacityUnits': desired_capacity,
                'WriteCapacityUnits': desired_capacity
            },
            'TableSizeBytes': 0, 'ItemCount': 0,
            'TableArn':
                'arn:aws:dynamodb:us-east-1:123456789011:table/TestTable',
            'LocalSecondaryIndexes': [],
            'GlobalSecondaryIndexes': [
                {
                    'IndexName': 'TestIndexName',
                    'KeySchema': [{'AttributeName': 'bu', 'KeyType': 'HASH'}],
                    'Projection': {'ProjectionType': 'ALL'},
                    'ProvisionedThroughput': {
                        'ReadCapacityUnits': desired_capacity,
                        'WriteCapacityUnits': desired_capacity
                    }
                }
            ]
        },
        'ResponseMetadata': {
            'HTTPStatusCode': 200,
            'HTTPHeaders': {
                'Content-Type': 'text/plain',
                'server': 'amazon.com'
            },
            'RetryAttempts': 0}
    }


def _table_description(
        index_name,
        table_name,
        current_table_capacity,
        current_gsi_capacity):
    return {
        'Table': {
            'AttributeDefinitions': [
                {'AttributeName': 'id', 'AttributeType': 'S'},
                {'AttributeName': 'bu', 'AttributeType': 'S'}
            ],
            'TableName': table_name,
            'KeySchema': [{'AttributeName': 'id', 'KeyType': 'HASH'}],
            'TableStatus': 'ACTIVE',
            'CreationDateTime':
                datetime.datetime(2017, 7, 17, 14, 26, 42, 517078,
                                  tzinfo=tzlocal()),
            'ProvisionedThroughput': {
                'ReadCapacityUnits': current_table_capacity,
                'WriteCapacityUnits': current_table_capacity
            },
            'TableSizeBytes': 0,
            'ItemCount': 0,
            'TableArn':
                'arn:aws:dynamodb:us-east-1:123456789011:table/TestTable',
            'LocalSecondaryIndexes': [],
            'GlobalSecondaryIndexes': [
                {
                    'IndexName': index_name,
                    'KeySchema': [{'AttributeName': 'bu', 'KeyType': 'HASH'}],
                    'Projection': {'ProjectionType': 'ALL'},
                    'ProvisionedThroughput': {
                        'ReadCapacityUnits': current_gsi_capacity,
                        'WriteCapacityUnits': current_gsi_capacity
                    }
                }
            ]
        },
        'ResponseMetadata': {
            'HTTPStatusCode': 200,
            'HTTPHeaders': {
                'Content-Type': 'text/plain',
                'server': 'amazon.com'
            },
            'RetryAttempts': 0}
    }


def test_update_dynamo_capacity_if_gsi_capacity_is_equal_to_desired():
    table_name = 'TestTable'
    desired_capacity = 1
    message = {'dynamo_table_name': table_name,
               'desired_capacity': desired_capacity}
    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account:dynamo_capacity_requests:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b280ff49-790d-5325-aab3-aead8e4f7524',
                'TopicArn':
                    'arn:aws:sns:region:account:dynamo_capacity_requests',
                'Subject': None,
                'Message': json.dumps(message),
                'Timestamp': '2017-07-16T05:09:22.942Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'MessageAttributes': {}
            }
        }]
    }

    table_description = _table_description(
        index_name="TestIndexName",
        table_name=table_name,
        current_table_capacity=300,
        current_gsi_capacity=desired_capacity
    )

    update_result = _update_response(desired_capacity)

    dynamo_client = MagicMock()
    dynamo_client.describe_table.return_value = table_description
    dynamo_client.update_table.return_value = update_result

    update_dynamo_capacity.update_dynamo_capacity(dynamo_client, event)

    dynamo_client.update_table.assert_called_once_with(
        TableName=table_name,
        ProvisionedThroughput={
            'ReadCapacityUnits': desired_capacity,
            'WriteCapacityUnits': desired_capacity
        }
    )


def test_update_dynamo_capacity_if_table_capacity_is_equal_to_desired():
    table_name = 'TestTable'
    index_name = 'TestIndexName'
    desired_capacity = 1
    message = {'dynamo_table_name': table_name,
               'desired_capacity': desired_capacity}
    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account:dynamo_capacity_requests:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b280ff49-790d-5325-aab3-aead8e4f7524',
                'TopicArn':
                    'arn:aws:sns:region:account:dynamo_capacity_requests',
                'Subject': None,
                'Message': json.dumps(message),
                'Timestamp': '2017-07-16T05:09:22.942Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'MessageAttributes': {}
            }
        }]
    }

    table_description = _table_description(
        index_name=index_name,
        table_name=index_name,
        current_table_capacity=desired_capacity,
        current_gsi_capacity=300
    )

    update_result = _update_response(desired_capacity=desired_capacity)

    dynamo_client = MagicMock()
    dynamo_client.describe_table.return_value = table_description
    dynamo_client.update_table.return_value = update_result

    update_dynamo_capacity.update_dynamo_capacity(dynamo_client, event)

    dynamo_client.update_table.assert_called_once_with(
        TableName=table_name,
        GlobalSecondaryIndexUpdates=[{
            'Update': {
                'IndexName': index_name,
                'ProvisionedThroughput': {
                    'ReadCapacityUnits': desired_capacity,
                    'WriteCapacityUnits': desired_capacity
                }
            }
        }]
    )


def test_update_dynamo_capacity_if_both_capacity_different_from_desired():
    table_name = 'TestTable'
    index_name = 'TestIndexName'
    desired_capacity = 1
    message = {'dynamo_table_name': table_name,
               'desired_capacity': desired_capacity}
    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account:dynamo_capacity_requests:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b280ff49-790d-5325-aab3-aead8e4f7524',
                'TopicArn':
                    'arn:aws:sns:region:account:dynamo_capacity_requests',
                'Subject': None,
                'Message': json.dumps(message),
                'Timestamp': '2017-07-16T05:09:22.942Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'MessageAttributes': {}
            }
        }]
    }

    table_description = _table_description(
        index_name=index_name,
        table_name=index_name,
        current_table_capacity=300,
        current_gsi_capacity=300
    )

    update_result = _update_response(desired_capacity=desired_capacity)

    dynamo_client = MagicMock()
    dynamo_client.describe_table.return_value = table_description
    dynamo_client.update_table.return_value = update_result

    update_dynamo_capacity.update_dynamo_capacity(dynamo_client, event)

    dynamo_client.update_table.assert_called_once_with(
        TableName=table_name,
        GlobalSecondaryIndexUpdates=[{
            'Update': {
                'IndexName': index_name,
                'ProvisionedThroughput': {
                    'ReadCapacityUnits': desired_capacity,
                    'WriteCapacityUnits': desired_capacity
                }
            }
        }],
        ProvisionedThroughput={
            'ReadCapacityUnits': desired_capacity,
            'WriteCapacityUnits': desired_capacity
        }
    )


def test_update_dynamo_capacity_if_both_capacity_equal_to_desired():
    table_name = 'TestTable'
    index_name = 'TestIndexName'
    desired_capacity = 1
    message = {'dynamo_table_name': table_name,
               'desired_capacity': desired_capacity}
    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account:dynamo_capacity_requests:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b280ff49-790d-5325-aab3-aead8e4f7524',
                'TopicArn':
                    'arn:aws:sns:region:account:dynamo_capacity_requests',
                'Subject': None,
                'Message': json.dumps(message),
                'Timestamp': '2017-07-16T05:09:22.942Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'MessageAttributes': {}
            }
        }]
    }

    table_description = _table_description(
        index_name=index_name,
        table_name=index_name,
        current_table_capacity=1,
        current_gsi_capacity=1
    )

    update_result = _update_response(desired_capacity=desired_capacity)

    dynamo_client = MagicMock()
    dynamo_client.describe_table.return_value = table_description
    dynamo_client.update_table.return_value = update_result

    update_dynamo_capacity.update_dynamo_capacity(dynamo_client, event)

    dynamo_client.update_table.assert_not_called()
