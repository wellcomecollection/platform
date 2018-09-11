# -*- encoding: utf-8

import os


class _PrefixedError(Exception):

    def __init__(self, message):
        prefix = os.environ.get(f'ERROR_{self.var_name}', self.default)
        super().__init__(f'[{prefix}] {message}')


class BadRequestError(_PrefixedError):
    """
    Raised if the user makes a malformed request.  Becomes an HTTP 400 error.
    """
    var_name = 'BAD_REQUEST'
    default = 'BadRequest'


class NotFoundError(_PrefixedError):
    """
    Raised if the user looks up a non-existent item.  Becomes an HTTP 404.
    """
    var_name = 'NOT_FOUND'
    default = 'NotFound'


class MethodNotAllowedError(_PrefixedError):
    """
    Raised if the user uses a disallowed method.  Becomes an HTTP 405 error.
    """
    var_name = 'METHOD_NOT_ALLOWED'
    default = 'MethodNotAllowed'
