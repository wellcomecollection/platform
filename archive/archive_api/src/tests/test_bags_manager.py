# -*- encoding: utf-8

import betamax
import pytest
import requests

from bags_manager import BagsManager, BagNotFoundError, BagServiceError


@pytest.fixture(scope="session")
def sess(pytestconfig):
    with betamax.Betamax.configure() as config:
        config.cassette_library_dir = str(
            pytestconfig.rootdir.join("src", "tests", "cassettes")
        )

    session = requests.Session()
    with betamax.Betamax(session) as vcr:
        vcr.use_cassette("test_bags_manager")
        yield session


@pytest.fixture(scope="session")
def bags_manager(sess):
    return BagsManager(endpoint="http://localhost:6001", sess=sess)


def test_can_lookup_existing_id(bags_manager, space_name, external_identifier):
    result = bags_manager.lookup_bag(space=space_name, id=external_identifier)
    assert result["id"] == {
        "space": space_name,
        "externalIdentifier": external_identifier,
    }


def test_404_is_not_found(bags_manager):
    with pytest.raises(BagNotFoundError):
        bags_manager.lookup_bag(space="nomespace", id="someid")


def test_not_200_or_404_is_error(bags_manager, space_name):
    with pytest.raises(BagServiceError):
        bags_manager.lookup_bag(space=space_name, id="b24923333-a")
