# -*- encoding: utf-8

import pytest
import main as flaskr


@pytest.fixture
def client():
    client = flaskr.app.test_client()
    yield client


def test_hello_world(client):
    rv = client.get('/')
    print(rv)
    assert False
