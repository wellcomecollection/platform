import os

BUCKET_NAME = os.getenv(key='BUCKET_NAME', default='vhs-bucket')
TABLE_NAME = os.getenv(key='TABLE_NAME', default='vhs-table')
REGION = os.getenv(key='REGION', default='eu-west-1')
