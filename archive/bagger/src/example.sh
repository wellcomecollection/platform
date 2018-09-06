# asset sources
export METS_FILESYSTEM_ROOT=''
export METS_BUCKET_NAME='xxxx'
export READ_METS_FROM_FILESHARE='False'
export WORKING_DIRECTORY='/tmp/_bagger'
export DROP_BUCKET_NAME='yyyy'
export DROP_BUCKET_NAME_METS_ONLY='yyyy-mets-only'
export CURRENT_PRESERVATION_BUCKET='bbbb'
export DLCS_SOURCE_BUCKET='dddd'
# aws
export AWS_ACCESS_KEY_ID='xxxx'
export AWS_SECRET_ACCESS_KEY='xxxx'
export AWS_DEFAULT_REGION='eu-west-1'
# DLCS config
export DLCS_ENTRY='https://api.dlcs.io/'
export DLCS_API_KEY='xxxx'
export DLCS_API_SECRET='xxxx'
export DLCS_CUSTOMER_ID='99'
export DLCS_SPACE='99'
# DDS credentials
export DDS_API_KEY='xxxx'
export DDS_API_SECRET='xxxx'
export DDS_ASSET_PREFIX='...'

python bagger.py $1