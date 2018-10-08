# -*- encoding: utf-8

import json

from helpers import assert_is_error_response


def test_lookup_bag(
    client, dynamodb_resource, s3_client, guid, bucket_bag, table_name_bag
):
    stored_bag = {"id": guid}

    s3_client.put_object(Bucket=bucket_bag, Key=guid, Body=json.dumps(stored_bag))

    table = dynamodb_resource.Table(table_name_bag)
    table.put_item(
        Item={"id": guid, "location": {"key": guid, "namespace": bucket_bag}}
    )

    resp = client.get(f"/storage/v1/bags/{guid}")
    assert resp.status_code == 200
    assert json.loads(resp.data) == {
        "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
        "id": guid,
    }


def test_lookup_missing_item_is_404(client, guid):
    resp = client.get(f"/storage/v1/bags/{guid}")
    assert_is_error_response(
        resp,
        status=404,
        description="Invalid id: No bag found for id=%r" % guid
    )
