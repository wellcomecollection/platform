# -*- encoding: utf-8

import pytest

import validators


class TestValidateSingleUrl:
    """
    Tests for ``validate_single_url``.
    """

    @pytest.mark.parametrize('bad_url', [
        'foo',
        'http://',
        '//foo',
    ])
    def test_missing_scheme_or_netloc_is_error(self, bad_url):
        with pytest.raises(ValueError, match='is not a complete URL'):
            validators.validate_single_url(bad_url)

    @pytest.mark.parametrize('good_url', [
        'https://example.org',
        'http://example.net/foo',
    ])
    def test_allows_valid_url(self, good_url):
        validators.validate_single_url(good_url)

    @pytest.mark.parametrize('bad_url', [
        'ftp://example.org/foo',
        'https://example.net/bar',
        'http://',
    ])
    def test_checks_url_supported_scheme_if_provided(self, bad_url):
        with pytest.raises(ValueError, match='is not a supported scheme'):
            validators.validate_single_url(bad_url, supported_schemes=['s3'])

    @pytest.mark.parametrize('good_url', [
        's3://example-bukkit/example.zip',
        'https://example.net/foo',
    ])
    def test_allows_okay_supported_scheme(self, good_url):
        validators.validate_single_url(
            good_url,
            supported_schemes=['s3', 'https']
        )

    @pytest.mark.parametrize('bad_url', [
        'http://#fragment',
        'https://example.net#another_fragment',
    ])
    def test_if_allow_fragment_false_then_fragment_is_error(self, bad_url):
        with pytest.raises(ValueError, match='fragment is not allowed'):
            validators.validate_single_url(bad_url, allow_fragment=False)

    @pytest.mark.parametrize('good_url', [
        's3://example-bukkit/bucket.zip#fragment',
        'https://example.net#another_fragment',
    ])
    def test_allows_fragment_if_allow_fragment_true(self, good_url):
        validators.validate_single_url(good_url, allow_fragment=True)

    @pytest.mark.parametrize('good_url', [
        'https://example.org/foo#bar',
        's3://example-bukkit/bucket.zip#baz',
    ])
    def test_good_url_is_allowed(self, good_url):
        validators.validate_single_url(good_url)


class TestValidateUUID:
    """
    Tests for ``validate_uuid``.
    """

    @pytest.mark.parametrize('bad_id', [
        'foo',
        'bar',
        '1234567890',
        'hwta58gq',
    ])
    def test_bad_id_is_rejected(self, bad_id):
        with pytest.raises(ValueError, match='Invalid id'):
            validators.validate_uuid(bad_id)

    @pytest.mark.parametrize('good_id', [
        '6adf5859-ffed-4224-a4d2-768278eb3240',
        'EA851462-0EDB-452A-9B9E-CE163A927614',
    ])
    def test_good_id_is_allowed(self, good_id):
        validators.validate_uuid(good_id)
