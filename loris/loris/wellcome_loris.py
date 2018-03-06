# -*- encoding: utf-8 -*-
"""This file contains the code specific to the Wellcome Loris deployment.

Code in this file will usually be highly specialised, and is unlikely to
be of interest to other users -- anything generic should be submitted as
an upstream patch.

"""

from loris.resolver import TemplateHTTPResolver
from requests.exceptions import RequestException
from tenacity import retry, retry_if_exception_type, stop_after_attempt


class WellcomeTemplateHTTPResolver(TemplateHTTPResolver):

    # We currently store the old Miro images in an S3 bucket, and request
    # them over HTTP.  Occasionally we've seen the HTTP connections flake out,
    # which causes a user-facing 500 -- and reloading the page fixes it.
    #
    # This causes us to retry the request once, if it's some sort of
    # HTTP error.  We've been running this in prod for months, and that sort
    # of 500 essentially vanished.
    @retry(
        stop=stop_after_attempt(2),
        retry=retry_if_exception_type(RequestException)
    )
    def copy_to_cache(self, ident):
        super().copy_to_cache(self, ident)
