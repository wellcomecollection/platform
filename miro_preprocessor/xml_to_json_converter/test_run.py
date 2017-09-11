# -*- encoding: utf-8 -*-

import boto3
from moto import mock_s3
import pytest

import run


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


@mock_s3
def test_creates_txt_with_all_images_json(set_region):
    s3_client = boto3.client("s3")
    bucket = "test-bucket"
    source_key = "images-AAA.xml"
    destination_key = "images-AAA.txt"
    xml_file_contents = """
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

    expected_txt_file = b"""{"image_no_calc":"A0000001","image_int_default":null,"image_artwork_date_from":"01/01/2000","image_artwork_date_to":"31/12/2000"}
{"image_no_calc":"A0000002","image_int_default":null,"image_artwork_date_from":"01/02/2000","image_artwork_date_to":"13/12/2000","image_barcode":"10000000","image_creator":["Caspar Bauhin"]}
{"image_no_calc":"A0000003","image_artwork_date_from":"02/02/2000","image_artwork_date_to":"13/11/2000","image_image_desc":"Test Description of Image"}
"""

    s3_client.create_bucket(ACL="private", Bucket=bucket)
    s3_client.put_object(ACL="private", Bucket=bucket, Body=xml_file_contents, Key=source_key)

    run.main(bucket, source_key, destination_key)

    get_file_request = s3_client.get_object(Bucket=bucket, Key=destination_key)
    assert get_file_request['Body'].read() == expected_txt_file
