# -*- encoding: utf-8

def test_hello_world(client):
    rv = client.get('/')
    assert rv.status_code == 200
    assert rv.data == b'Hello world'
