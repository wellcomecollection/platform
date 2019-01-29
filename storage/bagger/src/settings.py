import os

# For running on Windows on the Wellcome network with domain credentials
METS_FILESYSTEM_ROOT = os.getenv("METS_FILESYSTEM_ROOT")

# for running against AWS using the synced METS bucket
METS_BUCKET_NAME = os.getenv("METS_BUCKET_NAME")
METS_ROOT_PREFIX = "mets/"
METS_ONLY_ROOT_PREFIX = "mets_only/"

READ_METS_FROM_FILESHARE = os.getenv("READ_METS_FROM_FILESHARE") == "True"

# Where to assemble bag contents before bagging, e.g., a temp directory
WORKING_DIRECTORY = os.getenv("WORKING_DIRECTORY")

# Assumes same credentials for reading METS and writing bags
DROP_BUCKET_NAME = os.getenv("DROP_BUCKET_NAME")
DROP_BUCKET_NAME_METS_ONLY = os.getenv("DROP_BUCKET_NAME_METS_ONLY")
DROP_BUCKET_NAME_ERRORS = os.getenv("DROP_BUCKET_NAME_ERRORS")

CURRENT_PRESERVATION_BUCKET = os.getenv("CURRENT_PRESERVATION_BUCKET")
DLCS_SOURCE_BUCKET = os.getenv("DLCS_SOURCE_BUCKET")

# AWS_ACCESS_KEY_ID = os.getenv("AWS_ACCESS_KEY_ID")
# AWS_SECRET_ACCESS_KEY = os.getenv("AWS_SECRET_ACCESS_KEY")
AWS_DEFAULT_REGION = os.getenv("AWS_DEFAULT_REGION")

BAGGING_QUEUE = os.getenv("BAGGING_QUEUE")
POLL_INTERVAL = 5

# for determining the origin of an object
DLCS_ENTRY = os.getenv("DLCS_ENTRY")
DLCS_API_KEY = os.getenv("DLCS_API_KEY")
DLCS_API_SECRET = os.getenv("DLCS_API_SECRET")
DLCS_CUSTOMER_ID = os.getenv("DLCS_CUSTOMER_ID")
DLCS_SPACE = os.getenv("DLCS_SPACE")

# for fetching assets held in the Euston Road storage adaptor
DDS_API_KEY = os.getenv("DDS_API_KEY")
DDS_API_SECRET = os.getenv("DDS_API_SECRET")
DDS_ASSET_PREFIX = os.getenv("DDS_ASSET_PREFIX")

# This is passed to bagit as the general bag metadata
BAG_INFO = {
    "Source-Organization": "Intranda GmbH",
    "Contact-Email": "support@intranda.com",
    "External-Description": "[TO BE REPLACED]",
    "External-Identifier": "[TO BE REPLACED]",
}

# Possibly to go in bag-info:

# "Internal-Sender-Identifier": "170131",             # goobi process id
# "Internal-Sender-Description": "12324_b_b24923333"  # goobi process title

# Notifier

BAGGING_COMPLETE_TOPIC_ARN = os.getenv("BAGGING_COMPLETE_TOPIC_ARN")

# Storage Migration

DYNAMO_TABLE = os.getenv("DYNAMO_TABLE")
MINIMUM_BAG_DATE = os.getenv("MINIMUM_BAG_DATE")

WELLCOME_API_CLIENT_ID = os.getenv("WELLCOME_API_CLIENT_ID")
WELLCOME_API_CLIENT_SECRET = os.getenv("WELLCOME_API_CLIENT_SECRET")

WELLCOME_API_TOKEN_ENDPOINT = os.getenv("WELLCOME_API_TOKEN_ENDPOINT")
WELLCOME_API_SCOPE = os.getenv("WELLCOME_API_SCOPE")

STORAGE_API_BAGS = os.getenv("STORAGE_API_BAGS")
STORAGE_API_INGESTS = os.getenv("STORAGE_API_INGESTS")

DDS_GOOBI_NOTIFICATION = os.getenv("DDS_GOOBI_NOTIFICATION")

DDS_PACKAGE_FILEINFO = os.getenv("DDS_PACKAGE_FILEINFO")
DDS_ALTOTEXT_FILEINFO = os.getenv("DDS_ALTOTEXT_FILEINFO")
DDS_ALLANNO_FILEINFO = os.getenv("DDS_ALLANNO_FILEINFO")
