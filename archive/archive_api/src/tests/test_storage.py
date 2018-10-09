# -*- encoding: utf-8

import json

import pytest

from storage import VHSNotFound, VHSError, read_from_vhs


def test_can_read_from_vhs(dynamodb_resource, vhs_table_name, s3_client, bucket):
    vhs_value = {"id": "123"}

    s3_client.put_object(Bucket=bucket, Key="123.txt", Body=json.dumps(vhs_value))

    table = dynamodb_resource.Table(vhs_table_name)
    table.put_item(
        Item={"id": "123", "location": {"key": "123.txt", "namespace": bucket}}
    )

    resp = read_from_vhs(dynamodb_resource, vhs_table_name, s3_client, bucket, id="123")
    assert resp == vhs_value


def test_dynamodb_error_is_vhserror(dynamodb_resource, s3_client):
    with pytest.raises(VHSError, match="Error reading from DynamoDB"):
        read_from_vhs(
            dynamodb_resource, "no-such-table", s3_client, "no-such-bucket", id="123"
        )


def test_missing_dynamodb_table_is_vhsnotfounderror(
    dynamodb_resource, vhs_table_name, s3_client
):
    with pytest.raises(VHSNotFound, match="123"):
        read_from_vhs(
            dynamodb_resource, vhs_table_name, s3_client, "no-such-bucket", id="123"
        )


def test_malformed_dynamodb_row_is_vhserror(
    dynamodb_resource, vhs_table_name, s3_client
):
    table = dynamodb_resource.Table(vhs_table_name)
    table.put_item(
        Item={"id": "123", "location": {"k_y": "123.txt", "n_m_s_a_e": "bukkit"}}
    )

    with pytest.raises(VHSError, match="Malformed item in DynamoDB"):
        read_from_vhs(dynamodb_resource, vhs_table_name, s3_client, "bukkit", id="123")


def test_missing_s3_object_is_vhserror(dynamodb_resource, vhs_table_name, s3_client):
    table = dynamodb_resource.Table(vhs_table_name)
    table.put_item(
        Item={"id": "123", "location": {"key": "123.txt", "namespace": "bukkit"}}
    )

    with pytest.raises(VHSError, match="Error retrieving from S3"):
        read_from_vhs(dynamodb_resource, vhs_table_name, s3_client, "bukkit", id="123")


def test_non_json_in_s3_is_vhserror(
    dynamodb_resource, vhs_table_name, s3_client, bucket
):
    s3_client.put_object(Bucket=bucket, Key="123.txt", Body="<<notJson>>")

    table = dynamodb_resource.Table(vhs_table_name)
    table.put_item(
        Item={"id": "123", "location": {"key": "123.txt", "namespace": bucket}}
    )

    with pytest.raises(VHSError, match="Error decoding S3 contents as JSON"):
        read_from_vhs(dynamodb_resource, vhs_table_name, s3_client, bucket, id="123")


@pytest.fixture
def vhs_table_name(dynamodb_client, random_name):
    table_name = random_name

    try:
        dynamodb_client.create_table(
            TableName=table_name,
            KeySchema=[{"AttributeName": "id", "KeyType": "HASH"}],
            AttributeDefinitions=[{"AttributeName": "id", "AttributeType": "S"}],
            ProvisionedThroughput={"ReadCapacityUnits": 1, "WriteCapacityUnits": 1},
        )
        dynamodb_client.get_waiter("table_exists").wait(TableName=table_name)
    except dynamodb_client.exceptions.ResourceInUseException:
        pass

    yield table_name

    dynamodb_client.delete_table(TableName=table_name)
