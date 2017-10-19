import boto3
from moto import mock_sns, mock_sqs
import pytest


def pytest_runtest_setup(item):
    set_region()


@pytest.fixture()
def set_region():
    # Without this, boto3 is complaining about not having a region defined
    # in tests (despite one being set in the Travis env variables and passed
    # into the image).
    # TODO: Investigate this properly.
    boto3.setup_default_session(region_name='eu-west-1')


@pytest.fixture()
def moto_start(set_region):
    mock_sns().start()
    mock_sqs().start()
    yield
    mock_sns().stop()
    mock_sqs().stop()
