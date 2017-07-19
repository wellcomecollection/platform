#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import os
import time

import os_utils



def main():
    now = time.time()
    max_age = 1 * 24 * 60 * 60
    tmp = "/tmp"

    # TODO: Rough braindump of stuff to do:
    #
    #   - Create a generator of (path, atime)'s
    #   - Stick the ones we don't delete in a list, then delete them
    #   - What if can't delete them?  Do we spin in a loop?
    #   - Argument parsing with docopt
    #

    for root, _, filenames in os.walk(tmp):
        for f in filenames:
            path = os.path.join(root, f)
            last_access_time = os.stat(path).st_atime
            if now - last_access_time > max_age:
                os_utils.delete(path)

    size = os_utils.get_directory_size(tmp)
    print(size)
    if size > 10:
        pass

    for root, dirnames, _ in os.walk(tmp):
        for directory in dirnames:
            path = os.path.join(root, directory)
            os_utils.delete_directory_if_empty(path)


if __name__ == "__main__":
    main()
