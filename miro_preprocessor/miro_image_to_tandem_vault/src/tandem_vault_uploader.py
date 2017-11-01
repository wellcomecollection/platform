import json
import os

import boto3

from wellcome_lambda_utils.miro_utils import MiroImage
from wellcome_lambda_utils import sns_utils

from tandem_vault_metadata import miro_collections
import tandem_vault_api
import sqs_utils


def upload_asset(tandem_vault_api, s3_client, src_bucket, miro_image):
    src_key = f"fullsize/{miro_image.image_path}.jpg"

    image = s3_client.get_object(
        Bucket=src_bucket,
        Key=src_key
    )['Body']

    upload_set_id = miro_collections[miro_image.collection].upload_set_id

    return tandem_vault_api.upload_image_to_tv(
        image,
        src_key,
        upload_set_id
    )


def main():
    sqs_client = boto3.client('sqs')
    s3_client = boto3.client('s3')
    sns_client = boto3.client('sns')

    queue_url = os.environ['QUEUE_URL']
    topic_arn = os.environ['TOPIC_ARN']

    tandem_vault_api_key = os.environ['TANDEM_VAULT_API_KEY']
    tandem_vault_api_url = os.environ['TANDEM_VAULT_API_URL']

    src_bucket = os.environ['IMAGE_SRC_BUCKET']

    api = tandem_vault_api.TandemVaultAPI(
        tandem_vault_api_url,
        tandem_vault_api_key
    )

    sqs_reader = sqs_utils.SQSReader(sqs_client, queue_url)

    for message in sqs_reader:
        outer_message = json.loads(message['Body'])
        image_info = json.loads(outer_message['Message'])

        miro_image = MiroImage(image_info)
        subject = outer_message['Subject']

        asset_data = upload_asset(
            api,
            s3_client,
            src_bucket,
            miro_image
        )

        asset_id = asset_data['id']

        sns_utils.publish_sns_message(
            sns_client=sns_client,
            message={
                'asset_id': asset_id,
                'image_info': image_info
            },
            topic_arn=topic_arn,
            subject=subject
        )

        sqs_reader.delete_current()


if __name__ == '__main__':
    main()
