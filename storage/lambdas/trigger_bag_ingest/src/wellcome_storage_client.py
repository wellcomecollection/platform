import requests
from requests_oauthlib import OAuth2Session
from oauthlib.oauth2 import BackendApplicationClient
import urllib.parse


class WellcomeStorageClient:
    """
    Client for the Wellcome Storage API
    """

    def __init__(self, api_url, oauth_details=None):
        self.api_url = api_url
        if oauth_details:
            self.session = self.oauth_session(
                oauth_details["token_url"],
                oauth_details["client_id"],
                oauth_details["client_secret"],
            )
        else:
            self.session = requests.Session()

    def oauth_session(self, token_url, client_id, client_secret):
        """
        Create a simple OAuth session
        """
        client = BackendApplicationClient(client_id=client_id)
        api_session = OAuth2Session(client=client)
        api_session.fetch_token(
            token_url=token_url, client_id=client_id, client_secret=client_secret
        )
        return api_session

    def ingest_payload(self, bag_path, ingest_bucket_name, space):
        """
        Generates an ingest bag payload.
        """
        return {
            "type": "Ingest",
            "ingestType": {"id": "create", "type": "IngestType"},
            "space": {"id": space, "type": "Space"},
            "sourceLocation": {
                "type": "Location",
                "provider": {"type": "Provider", "id": "aws-s3-standard"},
                "bucket": ingest_bucket_name,
                "path": bag_path,
            },
        }

    def ingests_endpoint(self):
        return urllib.parse.urljoin(self.api_url, "storage/v1/ingests")

    def ingest_endpoint(self, id):
        return urllib.parse.urljoin(self.api_url, "storage/v1/ingests/" + id)

    def ingest(self, bag_path, ingest_bucket_name, space):
        """
        Call the storage ingests api to ingest bags
        """
        ingests_endpoint = self.ingests_endpoint()
        response = self.session.post(
            ingests_endpoint,
            json=self.ingest_payload(bag_path, ingest_bucket_name, space),
        )
        status_code = response.status_code
        if status_code == 201:
            return response.headers.get("Location")
        else:
            raise RuntimeError(f"{ingests_endpoint} returned {status_code}", response)

    def get_ingest(self, ingest_id):
        """
        Call the storage ingests api to get state of an ingest
        """
        ingest_endpoint = self.ingest_endpoint(ingest_id)
        response = self.session.get(ingest_endpoint)
        status_code = response.status_code
        if status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"{ingest_endpoint} returned {status_code}", response)
