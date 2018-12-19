# -*- encoding: utf-8 -*-


def test_ingest(storage_client):
    response = storage_client.ingest("bag", "ingest-bucket", "space")
    assert response == "/ingests/123456"


def test_get_ingest(storage_client):
    response = storage_client.get_ingest("123456")
    assert response["id"] == "123456"
