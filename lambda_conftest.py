# -*- encoding: utf-8 -*-
"""
Global py.test configuration for our Lambdas.
"""

import boto3
import pytest


def pytest_runtest_setup(item):

    # Set a default region before we start running tests.
    #
    # Without this line, boto3 complains about not having a region defined
    # (despite one being passed in the Travis env variables/local config).
    # TODO: Investigate this properly.
    boto3.setup_default_session(region_name='eu-west-1')
