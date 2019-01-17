#!/usr/bin/env python3
# -*- encoding: utf-8
"""
Create a request to ingest a bag into storage

Usage: ingest_bag.py <BAG>... [--oauth-credentials=<OAUTH_CREDENTIALS>] [--bucket=<BUCKET_NAME>] [--storage-space=<SPACE_NAME>]  [--api=<API>]
       ingest_bag.py -h | --help

Arguments:
    BAG                    paths to BagIt locations to ingest

Examples:
    ingest_bag.py b22454408.zip

Options:
    --oauth-credentials=<OAUTH_CREDENTIALS> The location of the oauth credentials
                                            [default: ~/.wellcome-storage/oauth-credentials.json]
    --bucket=<BUCKET_NAME>                  The S3 bucket containing the bags.
                                            [default: wellcomecollection-storage-ingests]
    --storage-space=<SPACE_NAME>            The space to use when storing the bag
                                            [default: test]
    --api=<API>                             The ingests API endpoint to use
                                            [default: stage]
    -h --help                               Print this help message

OAuth details:
  Credentials are supplied in a file (default ~/.wellcome-storage/oauth-credentials.json) with the following Json

  {
    "token_url": "https://auth.wellcomecollection.org/oauth2/token"
    "client_id": "YOUR-CLIENT-ID"
    "client_secret": "YOUR-CLIENT-SECRET"
  }
"""

import os

import docopt
import json
from requests_oauthlib import OAuth2Session
from oauthlib.oauth2 import BackendApplicationClient


def oauth_details_from_file(filepath):
    """
    Obtain OAuth details from a file
    """
    with open(filepath, "r") as f:
        oauth_credentials = json.load(f)
    return oauth_credentials


def oauth_session(token_url, client_id, client_secret):
    """
    Create a simple OAuth session
    """
    client = BackendApplicationClient(client_id=client_id)
    api_session = OAuth2Session(client=client)
    api_session.fetch_token(
        token_url=token_url, client_id=client_id, client_secret=client_secret
    )
    return api_session


def archive_bag_api_messages(bag_paths, space, ingest_bucket):
    """
    Generates bag archive messages.
    """
    for bag_path in bag_paths:
        yield {
            "type": "Ingest",
            "ingestType": {"id": "create", "type": "IngestType"},
            "space": {"id": space, "type": "Space"},
            "sourceLocation": {
                "type": "Location",
                "provider": {"type": "Provider", "id": "aws-s3-standard"},
                "bucket": ingest_bucket,
                "path": bag_path,
            },
        }


def call_ingest_api(ingest_bucket_name, bag_paths, space, ingests_endpoint, session):
    """
    Call the storage ingests api to ingest bags
    """
    for message in archive_bag_api_messages(bag_paths, space, ingest_bucket_name):
        response = session.post(ingests_endpoint, json=message)
        status_code = response.status_code
        if status_code != 201:
            print_result(f"ERROR calling {ingests_endpoint} with {message}", response)
        else:
            print(f"{message} -> {ingests_endpoint} [{status_code}]")
            location = response.headers.get("Location")
            ingest = session.get(location)
            if location:
                print_result(location, ingest)


def print_result(description, result):
    """
    pretty print the result an ingest request
    """
    print(description)
    dumped_json = json.dumps(result.json(), indent=2)
    print(dumped_json)


def main():
    args = docopt.docopt(__doc__)

    bag_paths = args["<BAG>"]
    space = args["--storage-space"]

    oauth_filepath = os.path.expanduser(args["--oauth-credentials"])
    oauth_details = oauth_details_from_file(oauth_filepath)
    api_session = oauth_session(
        oauth_details["token_url"],
        oauth_details["client_id"],
        oauth_details["client_secret"],
    )

    ingest_bucket_name = args["--bucket"]
    ingests_endpoint = args["--api"]
    api_lookup = {
        "production": "https://api.wellcomecollection.org/storage/v1/ingests",
        "stage": "https://api-stage.wellcomecollection.org/storage/v1/ingests",
    }
    if ingests_endpoint in api_lookup:
        ingests_endpoint = api_lookup[ingests_endpoint]

    call_ingest_api(ingest_bucket_name, bag_paths, space, ingests_endpoint, api_session)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        import sys

        sys.exit(1)
