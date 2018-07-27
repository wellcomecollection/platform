import os

# For running on Windows on the Wellcome network with domain credentials
# ignored if METS_BUCKET_NAME provided
METS_FILESYSTEM_ROOT = os.getenv("METS_FILESYSTEM_ROOT")

# for running against AWS using the synced METS bucket
METS_BUCKET_NAME = os.getenv("METS_BUCKET_NAME")
METS_ROOT_PREFIX = "mets/"

# Where to assemble bag contents before bagging, e.g., a temp directory
WORKING_DIRECTORY = os.getenv("WORKING_DIRECTORY")

# Assumes same credentials for reading METS and writing bags
DROP_BUCKET_NAME = os.getenv("DROP_BUCKET_NAME")

AWS_PUBLIC_KEY = os.getenv("AWS_PUBLIC_KEY")
AWS_SECRET_KEY = os.getenv("AWS_SECRET_KEY")
AWS_REGION = os.getenv("AWS_REGION")

# for determining the origin of an object
DLCS_ENTRY = os.getenv("DLCS_ENTRY")
DLCS_API_KEY = os.getenv("DLCS_API_KEY")
DLCS_SECRET = os.getenv("DLCS_SECRET")
DLCS_CUSTOMER_ID = os.getenv("DLCS_CUSTOMER_ID")
DLCS_SPACE = os.getenv("DLCS_SPACE")

# for fetching assets held in the Euston Road storage adaptor
DDS_API_KEY = os.getenv("DDS_API_KEY")
DDS_API_SECRET = os.getenv("DDS_API_SECRET")


