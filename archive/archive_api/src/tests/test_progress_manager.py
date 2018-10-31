# -*- encoding: utf-8

import betamax
import pytest
import requests

from progress_manager import (
    ProgressManager,
    ProgressNotFoundError,
    ProgressServiceError,
)


@pytest.fixture(scope="session")
def sess(pytestconfig):
    with betamax.Betamax.configure() as config:
        config.cassette_library_dir = str(
            pytestconfig.rootdir.join("src", "tests", "cassettes")
        )

    session = requests.Session()
    with betamax.Betamax(session) as vcr:
        vcr.use_cassette("test_progress_manager")
        yield session


@pytest.fixture(scope="session")
def progress_manager(sess):
    # AWLC: Currently these cassettes were recorded by running against
    # an imitation progress app written in Flask.
    #
    # At some point, swap out the cassettes for genuine responses from
    # a real progress app.
    #
    return ProgressManager(endpoint="http://localhost:6002", sess=sess)


class TestCreateRequest:
    def test_not_201_from_service_is_error(self, progress_manager):
        with pytest.raises(ProgressServiceError):
            progress_manager.create_request(
                request_json={
                    "type": "Ingest"
                }
            )

    def test_create_request(self, progress_manager):
        (location, json) = progress_manager.create_request(
            request_json={
                "type": "Ingest",
                "ingestType": {
                    "id": "create",
                    "type": "IngestType"
                },
                "space": {
                    "id": "bububa",
                    "type": "Space"
                },
                "uploadUrl": "s3://wellcomecollection-workflow-export-bagit/b21508628.zip"
            }
        )
        assert 'id' in json
        assert isinstance(location, str)

    def test_can_create_request_with_callback(self, progress_manager):
        (location, json) = progress_manager.create_request(
            request_json={
                "type": "Ingest",
                "ingestType": {
                    "id": "create",
                    "type": "IngestType"
                },
                "space": {
                    "id": "bububa",
                    "type": "Space"
                },
                "uploadUrl": "s3://wellcomecollection-workflow-export-bagit/b21508628.zip",
                "callback": {
                    "uri": "http://callback.org"
                }
            }
        )
        assert 'id' in json
        assert 'callback' in json
        assert isinstance(location, str)


class TestLookupProgress:
    def test_can_lookup_existing_id(self, progress_manager):
        result = progress_manager.lookup_progress(id="15d76ac8-e92e-4fb5-aea4-7e5bc98dbc20")
        assert result['id'] == "15d76ac8-e92e-4fb5-aea4-7e5bc98dbc20"

    def test_not_200_or_404_is_error(self, progress_manager):
        with pytest.raises(ProgressServiceError):
            progress_manager.lookup_progress(id="bad_status-{bad_status}")

    def test_404_is_not_found(self, progress_manager):
        with pytest.raises(ProgressNotFoundError):
            progress_manager.lookup_progress(id="15d76ac8-e92e-4fb5-aea4-7e5bc98dbc21")

    def test_bad_json_is_error(self, progress_manager):
        with pytest.raises(ProgressServiceError):
            progress_manager.lookup_progress(id="notjson")
