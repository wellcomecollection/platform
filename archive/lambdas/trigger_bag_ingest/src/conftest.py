import pytest
import requests
from wellcome_storage_client import WellcomeStorageClient


@pytest.fixture(scope="session")
def storage_client(docker_services, docker_ip):
    endpoint_url = f'http://{docker_ip}:{docker_services.port_for("wiremock", 8080)}'
    docker_services.wait_until_responsive(
        timeout=10.0,
        pause=0.1,
        check=_is_responsive(endpoint_url, lambda r: r.status_code == 403),
    )
    yield WellcomeStorageClient(endpoint_url)


def _is_responsive(endpoint_url, condition):
    def is_responsive():
        try:
            resp = requests.get(endpoint_url)
            if condition(resp):
                return True
        except requests.exceptions.ConnectionError:
            return False

    return is_responsive
