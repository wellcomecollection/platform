# -*- encoding: utf-8 -*-

import csv
import pytest
from io import StringIO

from sorter_logic import Decision, InvalidCollectionException, sort_image


def _empty_id_exceptions():
    csvfile = StringIO(
        """miro_id,cold_store,tandem_vault,catalogue_api\n,V0000000,false,false,false,false,false"""
    )

    return csv.DictReader(csvfile)


def _empty_contrib_exceptions():
    csvfile = StringIO(
        """XA,XB,XC\nZZZ,ZZZ,ZZZ"""
    )

    return csv.DictReader(csvfile)


def collection_image_data(**kwargs):
    image_data = update_image_data(**kwargs)
    collection = kwargs.pop('collection', 'images-M')

    return collection, image_data


def image_with_no_info(collection, image_title):
    return collection_image_data(collection=collection,
                                 image_title=image_title,
                                 image_pub_title=None,
                                 image_pub_periodical=None,
                                 image_innopac_id="1234567")


def update_image_data(**kwargs):
    image_data = {
        "image_no_calc": "V1234567",
        "image_title": "Image Title",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "Y",
        "image_use_restrictions": "CC-BY",
        "image_general_use": "Y",
        "image_innopac_id": "12345678",
        "image_cleared": "Y",
        "image_source_code": "XXX"
    }
    image_data.update(kwargs)
    return image_data


def id_exception(**kwargs):
    exception = {
        "cold_store": "",
        "tandem_vault": "",
        "catalogue_api": "",
    }
    exception.update(kwargs)
    return exception


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='images-AS'),
    collection_image_data(collection='images-FP'),
    collection_image_data(collection='images-D'),
    collection_image_data(collection='images-F'),
    collection_image_data(collection='images-L', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='images-V', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='images-M', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='images-L', image_tech_captured_mode="videodisc"),
    collection_image_data(collection='images-V', image_tech_captured_mode="videodisc"),
    collection_image_data(collection='images-M', image_tech_captured_mode="videodisc"),
    image_with_no_info(collection='images-L', image_title=None),
    image_with_no_info(collection='images-V', image_title=None),
    image_with_no_info(collection='images-M', image_title=None),
    image_with_no_info(collection='images-L', image_title="-"),
    image_with_no_info(collection='images-V', image_title="-"),
    image_with_no_info(collection='images-M', image_title="-"),
    image_with_no_info(collection='images-L', image_title="--"),
    image_with_no_info(collection='images-V', image_title="--"),
    image_with_no_info(collection='images-M', image_title="--"),
    image_with_no_info(collection='images-L', image_title="Awaiting description"),
    image_with_no_info(collection='images-V', image_title="Awaiting description"),
    image_with_no_info(collection='images-M', image_title="Awaiting description"),
    image_with_no_info(collection='images-L', image_title="Awaiting removal from MIRO as duplicate"),
    image_with_no_info(collection='images-V', image_title="Awaiting removal from MIRO as duplicate"),
    image_with_no_info(collection='images-M', image_title="Awaiting removal from MIRO as duplicate"),
    image_with_no_info(collection='images-L', image_title="Awaiting captions and consent form"),
    image_with_no_info(collection='images-V', image_title="Awaiting captions and consent form"),
    image_with_no_info(collection='images-M', image_title="Awaiting captions and consent form"),
    image_with_no_info(collection='images-L', image_title="Awaiting catalogue details"),
    image_with_no_info(collection='images-V', image_title="Awaiting catalogue details"),
    image_with_no_info(collection='images-M', image_title="Awaiting catalogue details"),
    image_with_no_info(collection='images-L', image_title="Awaiting caption info"),
    image_with_no_info(collection='images-V', image_title="Awaiting caption info"),
    image_with_no_info(collection='images-M', image_title="Awaiting caption info"),
    image_with_no_info(collection='images-L', image_title="No info available about this object"),
    image_with_no_info(collection='images-V', image_title="No info available about this object"),
    image_with_no_info(collection='images-M', image_title="No info available about this object"),
    image_with_no_info(collection='images-L', image_title="No neg"),
    image_with_no_info(collection='images-V', image_title="No neg"),
    image_with_no_info(collection='images-M', image_title="No neg"),
])
def test_is_cold_store(collection, image_data):
    assert sort_image(collection, image_data, _empty_id_exceptions(), _empty_contrib_exceptions()) == [
        Decision.cold_store]


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='images-L', image_use_restrictions="None"),
    collection_image_data(collection='images-V', image_use_restrictions="None"),
    collection_image_data(collection='images-M', image_use_restrictions="None"),
    collection_image_data(collection='images-L', image_tech_scanned_date="02/03/2016", image_use_restrictions="None"),
    collection_image_data(collection='images-L', image_library_dept="Public programmes"),
    collection_image_data(collection='images-V', image_library_dept="Public programmes"),
    collection_image_data(collection='images-M', image_library_dept="Public programmes"),
    collection_image_data(collection='images-L',
                          image_tech_scanned_date="02/03/2016",
                          image_use_restrictions="Restricted"),
    collection_image_data(collection='images-L', image_use_restrictions=None),
    collection_image_data(collection='images-V', image_use_restrictions=None),
    collection_image_data(collection='images-M', image_use_restrictions=None),
    collection_image_data(collection='images-L', image_copyright_cleared="N"),
    collection_image_data(collection='images-V', image_copyright_cleared="N"),
    collection_image_data(collection='images-M', image_copyright_cleared="N"),
    collection_image_data(collection='images-L', image_general_use="N"),
    collection_image_data(collection='images-V', image_general_use="N"),
    collection_image_data(collection='images-M', image_general_use="N"),
    collection_image_data(collection='images-L', image_use_restrictions="Top-secret"),
    collection_image_data(collection='images-V', image_use_restrictions="Top-secret"),
    collection_image_data(collection='images-M', image_use_restrictions="Top-secret")
])
def test_is_tandem_vault(collection, image_data):
    assert sort_image(collection, image_data, _empty_id_exceptions(), _empty_contrib_exceptions()) == [
        Decision.tandem_vault]


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='images-L', image_cleared="N"),
    collection_image_data(collection='images-M', image_use_restrictions="CC-BY-NC-ND", image_innopac_id=None),
    collection_image_data(collection='images-L', image_use_restrictions="CC-BY-NC-ND", image_innopac_id=None),
    collection_image_data(collection='images-V', image_use_restrictions="CC-BY-NC-ND", image_innopac_id=None),
    collection_image_data(),
    collection_image_data(image_innopac_id="1234567x"),
    collection_image_data(image_title=None,
                          image_pub_title=None,
                          image_pub_periodical=None,
                          image_innopac_id="1234567x"),
    collection_image_data(image_title=None,
                          image_pub_title=None,
                          image_pub_periodical=None,
                          image_innopac_id="12345678"),
    collection_image_data(collection='images-L', image_tech_scanned_date="01/03/2016"),
    collection_image_data(collection='images-L', image_tech_scanned_date="29/02/2016"),
    collection_image_data(collection='images-L', image_use_restrictions="CC-0"),
    collection_image_data(collection='images-V', image_use_restrictions="CC-0"),
    collection_image_data(collection='images-M', image_use_restrictions="CC-0"),
    collection_image_data(collection='images-L', image_use_restrictions="CC-BY"),
    collection_image_data(collection='images-V', image_use_restrictions="CC-BY"),
    collection_image_data(collection='images-M', image_use_restrictions="CC-BY"),
    collection_image_data(collection='images-L', image_use_restrictions="CC-BY-NC"),
    collection_image_data(collection='images-V', image_use_restrictions="CC-BY-NC"),
    collection_image_data(collection='images-M', image_use_restrictions="CC-BY-NC"),
    collection_image_data(collection='images-L', image_use_restrictions="CC-BY-NC-ND"),
    collection_image_data(collection='images-V', image_use_restrictions="CC-BY-NC-ND"),
    collection_image_data(collection='images-M', image_use_restrictions="CC-BY-NC-ND"),
    collection_image_data(collection='images-L', image_use_restrictions="CC-BY-NC-ND", image_innopac_id=None,
                          image_cleared=None),
])
def test_is_catalogue_api(collection, image_data):
    assert sort_image(collection, image_data, _empty_id_exceptions(), _empty_contrib_exceptions()) == [
        Decision.catalogue_api]


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='images-M', image_use_restrictions="CC-BY-NC-ND", image_innopac_id="blahbluh",
                          image_cleared="N"),
    collection_image_data(collection='images-L', image_use_restrictions="CC-BY-NC-ND", image_innopac_id="blahbluh",
                          image_cleared=None),
    collection_image_data(collection='images-M', image_use_restrictions="CC-BY-NC-ND", image_innopac_id="blahbluh"),
    collection_image_data(collection='images-L', image_use_restrictions="CC-BY-NC-ND", image_innopac_id="blahbluh"),
    collection_image_data(collection='images-V', image_use_restrictions="CC-BY-NC-ND", image_innopac_id="blahbluh"),
])
def test_is_no_decision(collection, image_data):
    assert sort_image(collection, image_data, _empty_id_exceptions(), _empty_contrib_exceptions()) == [Decision.none]


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='images-L', image_tech_scanned_date="02/03/2016", image_cleared="N"),
    collection_image_data(collection='images-L', image_tech_scanned_date="02/03/2016", image_cleared=None),
])
def test_is_catalogue_api_and_tandem_vault(collection, image_data):
    assert sort_image(collection, image_data, _empty_id_exceptions(), _empty_contrib_exceptions()) == [
        Decision.tandem_vault, Decision.catalogue_api]


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='images-L', image_tech_scanned_date="02/03/2016"),
    collection_image_data(collection='images-L', image_tech_scanned_date="30/06/2018"),
])
def test_is_tandem_vault_and_catalogue_api(collection, image_data):
    assert sort_image(collection, image_data, _empty_id_exceptions(), _empty_contrib_exceptions()) == [
        Decision.tandem_vault, Decision.catalogue_api]


@pytest.mark.parametrize('collection, image_data, id_exceptions, expected_decisions', [
    ('images-L',
     update_image_data(image_no_calc="V0002006",
                       image_tech_scanned_date="02/03/2016"),
     [id_exception(miro_id="V0002006", cold_store="true")],
     [Decision.cold_store]),
    ('images-L',
     update_image_data(image_no_calc="V0002006",
                       image_library_dept="Archives and Manuscripts"),
     [id_exception(miro_id="V0002006", tandem_vault="true", catalogue_api="true")],
     [Decision.tandem_vault, Decision.catalogue_api]),
    ('images-L',
     update_image_data(image_no_calc="V0002006",
                       image_library_dept="Archives and Manuscripts"),
     [id_exception(miro_id="V0002006", tandem_vault="false", catalogue_api="true")],
     [Decision.catalogue_api])
])
def test_id_exceptions_should_override_rules(collection, image_data, id_exceptions, expected_decisions):
    assert sort_image(collection, image_data, id_exceptions, _empty_contrib_exceptions()) == expected_decisions


def foo_dummy_csv():
    csvfile = StringIO("")

    fieldnames = ['foo']
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerow({'foo': 'bar'})

    return csv.DictReader(csvfile)


def _create_csv(s):
    csvfile = StringIO(s)
    return csv.DictReader(csvfile)


@pytest.mark.parametrize('collection, image_data, contrib_exceptions, expected_decisions', [
    ('images-A',
     update_image_data(image_source_code="FOO"),
     _create_csv("""A,B\nAAA,BBB\nFOO,CCC"""),
     [Decision.catalogue_api]),
    ('images-A',
     update_image_data(image_source_code="FOO"),
     _create_csv("""A,B\nAAA,BBB\nDDD,CCC"""),
     [Decision.cold_store]),
    ('images-L',
     update_image_data(image_use_restrictions="CC-BY-NC-ND", image_innopac_id=None),
     _create_csv("""A,B\nAAA,BBB\nDDD,CCC"""),
     [Decision.catalogue_api])
])
def test_contrib_exceptions_should_override_rules(collection, image_data, contrib_exceptions, expected_decisions):
    assert sort_image(collection, image_data, _empty_id_exceptions(), contrib_exceptions) == expected_decisions


def test_raise_exception_if_collection_is_not_f_v_m_fp_as():
    collection, image_data = collection_image_data(collection="images-A")
    with pytest.raises(InvalidCollectionException):
        sort_image(collection, image_data, _empty_id_exceptions(), _empty_contrib_exceptions())
