# -*- encoding: utf-8

from urllib.parse import urlparse
from uuid import UUID


def validate_single_url(url, supported_schemes=None, allow_fragment=True):
    """
    Check that the string ``url`` is:

    *   Indeed a URL
    *   If ``supported_schemes`` is provided, that the URL scheme matches
        the provided list.
    *   If ``allow_fragment`` is False, that the URL has no fragment.

    Raises ``ValueError`` if the string fails validation.

    """
    parsed_url = urlparse(url)
    errors = []

    if (not parsed_url.scheme) or (not parsed_url.netloc):
        errors.append('is not a complete URL')

    if supported_schemes and (parsed_url.scheme not in supported_schemes):
        errors.append(f'{parsed_url.scheme!r} is not a supported scheme {supported_schemes!r}')

    if (not allow_fragment) and parsed_url.fragment:
        errors.append(f'{parsed_url.fragment!r} fragment is not allowed')

    if errors:
        raise ValueError(', '.join(errors))


def validate_uuid(s):
    """
    Check that the string ``s`` is a valid UUID.

    Raises ``ValueError`` if the string fails validation.

    """
    try:
        UUID(s)
    except ValueError:
        raise ValueError(f'Invalid id={s!r}')
