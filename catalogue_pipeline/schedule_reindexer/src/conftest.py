from moto import mock_sns, mock_sqs
import pytest


@pytest.fixture()
def moto_start():
    mock_sns().start()
    mock_sqs().start()
    yield
    mock_sns().stop()
    mock_sqs().stop()
