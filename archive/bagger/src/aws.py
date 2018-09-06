import os
import boto3
import settings

boto_session = None


def get_boto_session():
    global boto_session
    if boto_session is None:
        boto_session = boto3.Session(region_name=settings.AWS_DEFAULT_REGION)
    return boto_session


def get_s3():
    return get_boto_session().resource("s3")


def upload(source, key):
    client = get_boto_session().client("s3")
    client.upload_file(source, settings.DROP_BUCKET_NAME, key)


def get_mets_xml(s3_path):
    obj = get_s3().Object(settings.METS_BUCKET_NAME, s3_path)
    xml_string = obj.get()["Body"].read().decode("utf-8")
    return xml_string


def save_mets_to_side(b_number, local_tmp_file):
    client = get_boto_session().client("s3")
    key = "{0}/{1}".format(b_number, os.path.basename(local_tmp_file))
    client.upload_file(local_tmp_file, settings.DROP_BUCKET_NAME_METS_ONLY, key)
