# -*- encoding: utf-8 -*-

import errno
import os
import subprocess
import sys


def delete(path):
    """
    Delete a file at a given path.

    :param path: File to delete.
    """
    print(f'Deleting file {path}')
    try:
        os.unlink(path)
    except PermissionError as err:
        print(f'Failed to delete {path}: {err}', file=sys.stderr)


def delete_directory_if_empty(path):
    """
    Delete a directory, but only if it's empty.  Raises OSError if the
    deletion fails.

    :param path: Directory to delete.
    """
    try:
        os.rmdir(path)
        print(path)
    except OSError as err:
        if err.errno == errno.ENOTEMPTY:
            pass
        else:
            raise


def get_directory_size(path):
    """
    Returns the number of 1-Kbyte blocks in a directory.

    :param path: Directory to size.
    """
    # The parameters to du are as follows:
    #
    #   -s  Display one entry for each file (in practice, just the directory)
    #   -k  Display block counts in 1-Kbyte blocks
    #   -H  Follow symlinks
    #
    # The output is of the form:
    #
    #   18016	/tmp
    #
    result = subprocess.check_output(["du", "-skH", path])
    return int(result.split()[0])
