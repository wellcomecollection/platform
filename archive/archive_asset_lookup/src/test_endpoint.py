# import unittest
# import boto3
# from moto import mock_dynamodb2, mock_s3
# from endpoint import main
# import settings
#
#
# class TestLambda(unittest.TestCase):
#
#     @mock_s3
#     @mock_dynamodb2
#     def test_lambda(self):
#
#         identifier = 'b12345678x'
#
#         bucket_name = settings.BUCKET_NAME
#
#         table_name = settings.TABLE_NAME
#
#         s3 = boto3.resource('s3')
#         bucket = s3.Bucket(bucket_name)
#         bucket.create()
#         bucket.put_object(
#             Key=identifier,
#             Body='storage manifest content'
#         )
#
#         dynamodb_client = boto3.client('dynamodb', region_name=settings.REGION)
#         dynamodb_client.create_table(
#             TableName=table_name,
#             KeySchema=[
#                 {
#                     'AttributeName': 'id',
#                     'KeyType': 'HASH'
#                 }
#             ],
#             AttributeDefinitions=[
#                 {
#                     'AttributeName': 'id',
#                     'AttributeType': 'S'
#                 },
#                 {
#                     'AttributeName': 's3key',
#                     'AttributeType': 'S'
#                 }
#             ],
#             ProvisionedThroughput={
#                 'ReadCapacityUnits': 20,
#                 'WriteCapacityUnits': 60
#             }
#         )
#
#         dynamodb_client.put_item(
#             TableName=table_name,
#             Item={
#                 'id': {
#                     'S': identifier
#                 },
#                 's3key': {
#                     'S': identifier
#                 }
#             }
#         )
#
#         event = {
#             "id": identifier
#         }
#
#         result = main(event, None)
#
#         self.assertEqual(result, "storage manifest content")
#
#
# if __name__ == '__main__':
#     unittest.main()
