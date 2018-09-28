# -*- encoding: utf-8

from datetime import datetime, timezone

from botocore.exceptions import ClientError

from validators import validate_uuid


class IngestProgress(object):
    static_fields = {
        '@context': 'https://api.wellcomecollection.org/storage/v1/context.json',
        'type': 'Ingest',
        'description': 'Ingest requested',
        'ingestType': {'id': 'create', 'type': 'IngestType'}
    }

    def __init__(self, id, bag_url, callback_url=None):
        try:
            validate_uuid(id)
        except ValueError:
            raise ValueError(
                f'Cannot create IngestProgress.  id={id!r} is not a valid ID.'
            )

        self.id = id

        self.uploadUrl = bag_url

        if callback_url:
            self.callbackUrl = callback_url

        self.createdDate = self.nowIsoFormatted()
        self.lastModifiedDate = self.nowIsoFormatted()

    def dict_with_static_data(self):
        """
        Merges dynamic fields with static fields to create a dict
        representing initialised progress data.
        """
        return {**self.__dict__, **self.static_fields}

    def nowIsoFormatted(self):
        """
        Creates an ISO 8601 date string *with* timezone
          Timezone is '+00:00' rather than 'Z'
        """
        return datetime.now(timezone.utc).isoformat()


def create_ingest_progress(ingest_progress, dynamodb_resource, table_name):
    """
    Creates an ingest progress record
    """
    table = dynamodb_resource.Table(table_name)
    try:
        table.put_item(
            Item=ingest_progress.dict_with_static_data(),
            ConditionExpression='attribute_not_exists(id)'
        )
    except ClientError as e:
        if e.response['Error']['Code'] == 'ConditionalCheckFailedException':
            raise ValueError(f"Cannot create IngestProgress, id already exists '{ingest_progress.id}'.")
        else:
            raise
