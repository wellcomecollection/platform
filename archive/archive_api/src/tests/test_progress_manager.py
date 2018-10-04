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
    return ProgressManager(endpoint="http://localhost:6000", sess=sess)


class TestCreateRequest:
    @pytest.mark.parametrize("bad_status", [200, 400, 404, 500])
    def test_not_202_from_service_is_error(self, bad_status, progress_manager):
        with pytest.raises(ProgressServiceError, match="Expected HTTP 202"):
            progress_manager.create_request(
                upload_url=f"http://example.org/?status={bad_status}", callback_url=None
            )

    def test_missing_location_header_is_error(self, progress_manager):
        with pytest.raises(ProgressServiceError, match="No Location header"):
            progress_manager.create_request(
                upload_url="http://example.org/?location=no", callback_url=None
            )

    def test_create_request(self, progress_manager):
        result = progress_manager.create_request(
            upload_url="http://example.org/?id=123", callback_url=None
        )
        assert result == "123"

    def test_can_create_request_with_callback(self, progress_manager):
        result = progress_manager.create_request(
            upload_url="http://example.org/?id=567",
            callback_url="http://callback.net/?id=b567",
        )
        assert result == "567"


class TestLookupProgress:
    def test_can_lookup_existing_id(self, progress_manager):
        result = progress_manager.lookup_progress(id="123")
        assert result == {"progress": "123"}

    @pytest.mark.parametrize("bad_status", [202, 400, 500])
    def test_not_200_or_404_is_error(self, bad_status, progress_manager):
        with pytest.raises(ProgressServiceError, match="Expected HTTP 200 or 404"):
            progress_manager.lookup_progress(id="bad_status-{bad_status}")

    def test_404_is_not_found(self, progress_manager):
        with pytest.raises(ProgressNotFoundError):
            progress_manager.lookup_progress(id="bad_status-404")

    def test_bad_json_is_error(self, progress_manager):
        with pytest.raises(ProgressServiceError):
            progress_manager.lookup_progress(id="notjson")
