# -*- encoding: utf-8

import os
import uuid

import betamax
import pytest
import requests


@pytest.fixture(scope="session")
def recorded_sess(pytestconfig):
    with betamax.Betamax.configure() as config:
        config.cassette_library_dir = str(
            pytestconfig.rootdir.join("src", "tests", "cassettes")
        )

    session = requests.Session()
    with betamax.Betamax(session) as vcr:
        vcr.use_cassette("test_archive_api")
        yield session


@pytest.fixture
def client(sns_client, topic_arn, recorded_sess):
    # This only has to work when populating the betamax recording file;
    # although we run on Linux in Travis CI, this will still fine because
    # we use the cached recordings.
    os.environ.update(
        {"PROGRESS_MANAGER_ENDPOINT": "http://docker.for.mac.localhost:6000"}
    )
    os.environ.update({"BAGS_MANAGER_ENDPOINT": "http://host.docker.internal:6001"})

    from archive_api import app

    app.config["SNS_CLIENT"] = sns_client
    app.config["SNS_TOPIC_ARN"] = topic_arn
    app.config["PROGRESS_MANAGER"].sess = recorded_sess
    app.config["BAGS_MANAGER"].sess = recorded_sess

    yield app.test_client()


@pytest.fixture
def guid():
    return str(uuid.uuid4())


@pytest.fixture
def external_identifier():
    return "b22454408"


@pytest.fixture
def space_name():
    return "digitised"


@pytest.fixture
def bag_id(external_identifier, space_name):
    return f"{space_name}/{external_identifier}"
