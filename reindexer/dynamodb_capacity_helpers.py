# -*- encoding: utf-8

import attr
import boto3
from botocore.exceptions import ClientError


@attr.s
class DynamoDBCapacity(object):
    write = attr.ib()
    read = attr.ib()

    def __str__(self):
        return f'{self.write} write and {self.read} read'


def _get_max_dynamodb_capacity(resource_id, dimension):
    autoscaling = boto3.client('application-autoscaling')

    resp = autoscaling.describe_scalable_targets(
        ServiceNamespace='dynamodb',
        ResourceIds=[resource_id],
        ScalableDimension=f'dynamodb:{dimension}'
    )
    assert len(resp['ScalableTargets']) == 1, resp
    return resp['ScalableTargets'][0]['MaxCapacity']


def get_dynamodb_max_table_capacity(table_name):
    write_capacity = _get_max_dynamodb_capacity(
        resource_id=f'table/{table_name}',
        dimension='table:WriteCapacityUnits'
    )

    read_capacity = _get_max_dynamodb_capacity(
        resource_id=f'table/{table_name}',
        dimension='table:ReadCapacityUnits'
    )

    return DynamoDBCapacity(write=write_capacity, read=read_capacity)


def get_dynamodb_max_gsi_capacity(table_name, gsi_name):
    write_capacity = _get_max_dynamodb_capacity(
        resource_id=f'table/{table_name}/index/{gsi_name}',
        dimension='index:WriteCapacityUnits'
    )

    read_capacity = _get_max_dynamodb_capacity(
        resource_id=f'table/{table_name}/index/{gsi_name}',
        dimension='index:ReadCapacityUnits'
    )

    return DynamoDBCapacity(write=write_capacity, read=read_capacity)


def set_dynamodb_table_capacity(table_name, desired_capacity):
    dynamodb = boto3.client('dynamodb')
    try:
        dynamodb.update_table(
            TableName=table_name,
            ProvisionedThroughput={
                'ReadCapacityUnits': desired_capacity.read,
                'WriteCapacityUnits': desired_capacity.write,
            },
        )
    except ClientError as err:
        if err.response['Error']['Message'].startswith(
            'The provisioned throughput for the table will not change. '
            'The requested value equals the current value.'
        ):
            pass
        else:
            raise


def set_dynamodb_gsi_capacity(table_name, gsi_name, desired_capacity):
    dynamodb = boto3.client('dynamodb')
    try:
        dynamodb.update_table(
            TableName=table_name,
            GlobalSecondaryIndexUpdates=[
                {
                    'Update': {
                        'IndexName': gsi_name,
                        'ProvisionedThroughput': {
                            'ReadCapacityUnits': desired_capacity.read,
                            'WriteCapacityUnits': desired_capacity.write,
                        }
                    }
                }
            ]
        )
    except ClientError as err:
        if err.response['Error']['Message'].startswith(
            f'The provisioned throughput for the index {gsi_name} will not '
            f'change. The requested value equals the current value.'
        ):
            pass
        else:
            raise
