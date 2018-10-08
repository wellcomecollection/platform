# -*- encoding: utf-8

import json


def test_returns_a_present_bag(
    client, dynamodb_resource, table_name_bag, s3_client, bucket_bag, guid
):
    stored_bag = {"id": guid}

    s3_client.put_object(Bucket=bucket_bag, Key=guid, Body=json.dumps(stored_bag))

    table = dynamodb_resource.Table(table_name_bag)
    table.put_item(
        Item={"id": guid, "location": {"key": guid, "namespace": bucket_bag}}
    )

    resp = client.get(f"/storage/v1/bags/{guid}")
    assert resp.status_code == 200

    rv = json.loads(resp.data)
    assert rv["id"] == guid
    assert (
        rv["@context"] == "https://api.wellcomecollection.org/storage/v1/context.json"
    )
    assert rv["type"] == "Bag"


def test_returns_500_if_s3_object_missing(
    client, dynamodb_resource, table_name_bag, bucket_bag, guid
):
    stored_bag = {"id": guid}

    table = dynamodb_resource.Table(table_name_bag)
    table.put_item(
        Item={"id": guid, "location": {"key": guid, "namespace": bucket_bag}}
    )

    resp = client.get(f"/storage/v1/bags/{guid}")
    assert resp.status_code == 500

    assert json.loads(resp.data) == {
        "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
        "errorType": "http",
        "httpStatus": 500,
        "label": "Internal Server Error",
        "type": "Error",
    }


def test_returns_500_if_malformed_dynamodb(
    client, dynamodb_resource, table_name_bag, guid
):
    table = dynamodb_resource.Table(table_name_bag)
    table.put_item(Item={"id": guid, "location": {"k_y": guid, "n_m_s_c_e": "bukkit"}})

    resp = client.get(f"/storage/v1/bags/{guid}")
    assert resp.status_code == 500

    assert json.loads(resp.data) == {
        "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
        "errorType": "http",
        "httpStatus": 500,
        "label": "Internal Server Error",
        "type": "Error",
    }


def test_returns_404_if_no_such_bag(client, guid):
    resp = client.get(f"/storage/v1/bags/{guid}")
    assert resp.status_code == 404

    assert json.loads(resp.data) == {
        "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
        "description": f"No bag found for id='{guid}'",
        "errorType": "http",
        "httpStatus": 404,
        "label": "Not Found",
        "type": "Error",
    }


def test_returns_405_if_try_to_post(client):
    resp = client.post(f"/storage/v1/bags/123")

    assert resp.status_code == 405
    assert json.loads(resp.data) == {
        "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
        "description": "The method is not allowed for the requested URL.",
        "errorType": "http",
        "httpStatus": 405,
        "label": "Method Not Allowed",
        "type": "Error",
    }
