import boto3
import settings

boto_session = None


def get_boto_session():
    global boto_session
    if boto_session is None:
        boto_session = boto3.Session(
            aws_access_key_id=settings.AWS_PUBLIC_KEY,
            aws_secret_access_key=settings.AWS_SECRET_KEY,
            region_name=settings.AWS_REGION
        )
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
