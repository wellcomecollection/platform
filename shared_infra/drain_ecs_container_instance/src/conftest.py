# -*- encoding: utf-8 -*-

import boto3
from moto import mock_ec2, mock_autoscaling, mock_ecs, mock_sns, mock_sqs
import pytest


@pytest.fixture()
def moto_start():
    mock_autoscaling().start()
    mock_ec2().start()
    mock_ecs().start()
    mock_sns().start()
    mock_sqs().start()
    yield
    mock_autoscaling().stop()
    mock_ec2().stop()
    mock_ecs().stop()
    mock_sns().stop()
    mock_sqs().stop()
