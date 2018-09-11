# -*- encoding: utf-8

import pytest

import ingest_manager


@pytest.fixture
def client():
    client = ingest_manager.app.test_client()
    yield client


def test_hello_world(client):
    rv = client.get('/')
    assert rv.status_code == 200
    assert rv.data == b'Hello world'
