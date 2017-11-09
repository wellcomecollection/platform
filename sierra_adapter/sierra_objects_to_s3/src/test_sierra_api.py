# -*- encoding: utf-8 -*-


def test_can_get_bibs_from_api(api):
    expected_length = 29

    bibs = api.get_objects("/bibs", params={
        'updatedDate': "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]"
    })

    actual_length = len(bibs)
    assert actual_length == expected_length

    results = list(bibs)
    assert len(results) == expected_length


def test_can_get_items_from_api(api):
    expected_length = 50

    bibs = api.get_objects("/items", {
        'updatedDate': "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]"
    })

    actual_length = len(bibs)
    assert actual_length == expected_length

    results = list(bibs)
    assert len(results) == expected_length
