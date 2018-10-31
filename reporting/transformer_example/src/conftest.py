import pytest
from elasticsearch import Elasticsearch


@pytest.fixture
def elasticsearch_client(elasticsearch_url):
    yield Elasticsearch(elasticsearch_url, http_auth=("elastic", "changeme"))
