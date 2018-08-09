import os

VHS_NAME = os.getenv(key='VHS_NAME', default='archive-registrar-storage-manifests')
VHS_BUCKET_PREFIX = os.getenv(key='VHS_BUCKET_PREFIX', default='wellcomecollection-vhs-')
VHS_TABLE_PREFIX = os.getenv(key='VHS_TABLE_PREFIX', default='vhs-')
AWS_REGION = os.getenv(key='AWS_REGION', default='eu-west-1')
