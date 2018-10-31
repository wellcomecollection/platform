# -*- encoding: utf-8

import os

import requests


class ArchiveAPIConfig(object):

    PROGRESS_MANAGER_SESSION = requests.Session()
    BAGS_MANAGER_SESSION = requests.Session()

    # Disable Flask-RESTPlus including the "message" field on errors.
    # See https://flask-restplus.readthedocs.io/en/stable/errors.html
    ERROR_INCLUDE_MESSAGE = False

    def __init__(self, development=False):
        try:
            if development:
                self.PROGRESS_MANAGER_ENDPOINT = "http://localhost:6000"
                self.BAGS_MANAGER_ENDPOINT = "http://localhost:6001"
            else:
                self.PROGRESS_MANAGER_ENDPOINT = os.environ["PROGRESS_MANAGER_ENDPOINT"]
                self.BAGS_MANAGER_ENDPOINT = os.environ["BAGS_MANAGER_ENDPOINT"]
        except KeyError as err:
            raise RuntimeError(f"Unable to create config: {err!r}")
