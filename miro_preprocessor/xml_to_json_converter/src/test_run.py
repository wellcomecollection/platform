# -*- encoding: utf-8 -*-

import boto3
from moto import mock_s3
import pytest

import run


@pytest.yield_fixture(scope="function")
def s3_fixture():
    mock_s3().start()

    client = boto3.client("s3")
    resource = boto3.resource("s3")

    yield client, resource

    mock_s3().stop()


@pytest.fixture()
def set_region():
    # Need this otherwise boto complains about missing region
    # in sns_utils.pblish_sns_message when trying to create client
    # with sns = boto3.client('sns') (despite region being set with
    # the environment variable AWS_DEFAULT_REGION, which should be
    # read by default by boto)
    # Weirdly enough it doesn't complain in this file when it tries
    # to do the same thing.
    # After investigation this is not related to moto
    session = boto3.Session()
    region = session.region_name
    boto3.setup_default_session(region_name=region)


@pytest.fixture()
def xml_file_contents(s3_fixture, set_region):
    s3_client = s3_fixture[0]
    bucket = "test-bucket"
    src_key = "images-AAA.xml"

    file_contents = """
    <?xml version="1.0" encoding="iso-8859-1" standalone="yes"?>
    <rmxml version="1.1">
        <image>
            <image_no_calc>A0000001</image_no_calc>
            <image_int_default></image_int_default>
            <image_artwork_date_from>01/01/2000</image_artwork_date_from>
            <image_artwork_date_to>31/12/2000</image_artwork_date_to>
        </image>
        <image>
            <image_no_calc>A0000002</image_no_calc>
            <image_int_default></image_int_default>
            <image_artwork_date_from>01/02/2000</image_artwork_date_from>
            <image_artwork_date_to>13/12/2000</image_artwork_date_to>
            <image_barcode>10000000</image_barcode>
            <image_creator>
                <_>Caspar Bauhin</_>
            </image_creator>
        </image>
        <image>
            <image_no_calc>A0000003</image_no_calc>
            <image_artwork_date_from>02/02/2000</image_artwork_date_from>
            <image_artwork_date_to>13/11/2000</image_artwork_date_to>
            <image_image_desc>Test Description of Image</image_image_desc>
        </image>
    </rmxml>"""

    s3_client.create_bucket(ACL="private", Bucket=bucket)
    s3_client.put_object(ACL="private", Bucket=bucket, Body=file_contents, Key=src_key)

    return {
        "bucket": bucket,
        "src_key": src_key,
        "file_contents": file_contents
    }


@pytest.fixture()
def wrong_sierra_number_xml_file_contents(s3_fixture, set_region):
    s3_client = s3_fixture[0]
    bucket = "test-bucket"
    src_key = "images-AAA.xml"

    file_contents = """
    <?xml version="1.0" encoding="iso-8859-1" standalone="yes"?>
    <rmxml version="1.1">
        <image>
            <image_innopac_id>113183382</image_innopac_id>
            <image_no_calc>L0001138EB</image_no_calc>
        </image>
        <image>
            <image_innopac_id>113183382</image_innopac_id>
            <image_no_calc>L0001138EA</image_no_calc>
        </image>
        <image>
            <image_innopac_id>150056628</image_innopac_id>
            <image_no_calc>L0035213</image_no_calc>
        </image>
    </rmxml>"""

    s3_client.create_bucket(ACL="private", Bucket=bucket)
    s3_client.put_object(ACL="private", Bucket=bucket, Body=file_contents, Key=src_key)

    return {
        "bucket": bucket,
        "src_key": src_key,
        "file_contents": file_contents
    }


def test_creates_txt_with_all_images_json(s3_fixture, wrong_sierra_number_xml_file_contents):
    s3_client = s3_fixture[0]
    bucket = wrong_sierra_number_xml_file_contents["bucket"]

    src_key = wrong_sierra_number_xml_file_contents["src_key"]
    dst_key = "images-AAA.txt"

    expected_txt_file = b"""{"collection":"images-AAA","image_data":{"image_innopac_id":"1318338x","image_no_calc":"L0001138EB"}}
{"collection":"images-AAA","image_data":{"image_innopac_id":"1318338x","image_no_calc":"L0001138EA"}}
{"collection":"images-AAA","image_data":{"image_innopac_id":"1500562x","image_no_calc":"L0035213"}}
"""

    run.main(bucket, src_key, dst_key)

    get_file_request = s3_client.get_object(Bucket=bucket, Key=dst_key)

    read = get_file_request['Body'].read()
    print(read)
    assert read == expected_txt_file


def test_fixes_errors_in_sierra_number(s3_fixture, xml_file_contents):
    s3_client = s3_fixture[0]
    bucket = xml_file_contents["bucket"]

    src_key = xml_file_contents["src_key"]
    dst_key = "images-AAA.txt"

    expected_txt_file = b"""{"collection":"images-AAA","image_data":{"image_artwork_date_from":"01/01/2000","image_artwork_date_to":"31/12/2000","image_int_default":null,"image_no_calc":"A0000001"}}
{"collection":"images-AAA","image_data":{"image_artwork_date_from":"01/02/2000","image_artwork_date_to":"13/12/2000","image_barcode":"10000000","image_creator":["Caspar Bauhin"],"image_int_default":null,"image_no_calc":"A0000002"}}
{"collection":"images-AAA","image_data":{"image_artwork_date_from":"02/02/2000","image_artwork_date_to":"13/11/2000","image_image_desc":"Test Description of Image","image_no_calc":"A0000003"}}
"""

    run.main(bucket, src_key, dst_key)

    get_file_request = s3_client.get_object(Bucket=bucket, Key=dst_key)

    assert get_file_request['Body'].read() == expected_txt_file


def test_creates_json_file_for_each_image(s3_fixture, xml_file_contents):
    s3_client = s3_fixture[0]
    bucket = xml_file_contents["bucket"]
    prefix = "json"

    dst_key = "images-AAA.txt"
    src_key = xml_file_contents["src_key"]

    expected_json_objects = {
        "json/A0000001.json": b'{"collection":"images-AAA","image_data":{"image_artwork_date_from":"01/01/2000","image_artwork_date_to":"31/12/2000","image_int_default":null,"image_no_calc":"A0000001"}}',
        "json/A0000003.json": b'{"collection":"images-AAA","image_data":{"image_artwork_date_from":"02/02/2000","image_artwork_date_to":"13/11/2000","image_image_desc":"Test Description of Image","image_no_calc":"A0000003"}}',
        "json/A0000002.json": b'{"collection":"images-AAA","image_data":{"image_artwork_date_from":"01/02/2000","image_artwork_date_to":"13/12/2000","image_barcode":"10000000","image_creator":["Caspar Bauhin"],"image_int_default":null,"image_no_calc":"A0000002"}}'
    }

    run.main(bucket, src_key, dst_key, prefix)

    response = s3_client.list_objects(
        Bucket=bucket,
        Prefix=prefix
    )

    keys = [obj["Key"] for obj in response["Contents"]]

    def _get_body(key):
        return s3_client.get_object(
            Bucket=bucket,
            Key=key
        )["Body"].read()

    actual_json_objects = {key: _get_body(key) for key in keys}

    assert expected_json_objects == actual_json_objects


def test_build_collection_id():
    src_key = "source/Images-V.xml"
    expected_collection_id = "Images-V"

    actual_collection_id = run._build_collection_id(src_key)

    assert expected_collection_id == actual_collection_id
