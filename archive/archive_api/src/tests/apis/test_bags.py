# -*- encoding: utf-8

import json

from helpers import assert_is_error_response


def test_returns_a_present_bag(client, bag_id):
    resp = client.get(f"/storage/v1/bags/{bag_id}")
    assert resp.status_code == 200

    rv = json.loads(resp.data)
    assert rv["id"] == bag_id
    assert (
        rv["@context"] == "https://api.wellcomecollection.org/storage/v1/context.json"
    )
    # TODO: Replace this assertion
    # assert rv["type"] == "Bag"


def test_returns_404_if_no_such_bag(client):
    bag_id = "somespace/someid"
    resp = client.get(f"/storage/v1/bags/{bag_id}")
    assert_is_error_response(
        resp, status=404, description=f"Invalid id: No bag found for id='{bag_id}'"
    )


def test_returns_405_if_try_to_post(client):
    resp = client.post(f"/storage/v1/bags/space/123")
    assert_is_error_response(
        resp, status=405, description="The method is not allowed for the requested URL."
    )
