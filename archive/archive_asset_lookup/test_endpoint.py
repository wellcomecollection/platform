import unittest
import boto3
from moto import mock_dynamodb2, mock_s3
from endpoint import lambda_handler
import settings


class TestLambda(unittest.TestCase):

    @mock_s3
    @mock_dynamodb2
    def test_lambda(self):

        identifier = 'b12345678x'

        s3 = boto3.resource('s3')
        bucket = s3.Bucket(settings.BUCKET_NAME)
        bucket.create()
        bucket.put_object(
            Key=identifier,
            Body='storage manifest content'
        )

        dynamodb_client = boto3.client('dynamodb', region_name=settings.AWS_REGION)
        dynamodb_client.create_table(
            TableName=settings.TABLE_NAME,
            KeySchema=[
                {
                    'AttributeName': 'identifier',
                    'KeyType': 'HASH'
                }
            ],
            AttributeDefinitions=[
                {
                    'AttributeName': 'identifier',
                    'AttributeType': 'S'
                },
                {
                    'AttributeName': 's3',
                    'AttributeType': 'S'
                }
            ],
            ProvisionedThroughput={
                'ReadCapacityUnits': 20,
                'WriteCapacityUnits': 60
            }
        )

        dynamodb_client.put_item(
            TableName=settings.TABLE_NAME,
            Item={
                'identifier': {
                    'S': identifier
                },
                's3': {
                    'S': 's3://{}/{}'.format(settings.BUCKET_NAME, identifier)
                }
            }
        )

        event = {
            "identifier": identifier
        }

        result = lambda_handler(event, None)

        self.assertEqual(result, "storage manifest content")


if __name__ == '__main__':
    unittest.main()
