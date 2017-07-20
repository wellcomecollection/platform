#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Purge cache based on age of files and the size of the cache.

Usage:
  cache_cleaner.py --path=<PATH> --max-age=<MAX_AGE> --max-size=<MAX_SIZE>
  cache_cleaner.py -h | --help

Options:
  -h --help                Show this screen.
  --path=<PATH>            Path of the cache to clean.
  --max-age=<MAX_AGE>      Delete files that are older than MAX_AGE days.
  --max-size=<MAX_SIZE>    Delete files until the cache size is less than MAX_SIZE Kbytes.

"""

import os
import time

import docopt

import os_utils


def main():
    args = docopt.docopt(__doc__)

    now = time.time()
    max_age = int(args['--max-age']) * 24 * 60 * 60
    cache_path = args['--path']
    max_cache_size = int(args['--max-size'])

    for path, last_access_time in os_utils.get_files(cache_path):
        if now - last_access_time > max_age:
            os_utils.delete(path)

    all_files = list(os_utils.get_files(cache_path))
    sorted_files = sorted(all_files, key=lambda x: x.access_time)
    while os_utils.get_directory_size(cache_path) > max_cache_size:
        try:
            os_utils.delete(sorted_files.pop(0).path)
        except IndexError:
            raise RuntimeError("No files left to delete but cache is still too large")

    for directory in os_utils.get_directories(path):
        os_utils.delete_directory_if_empty(path)


if __name__ == "__main__":
    main()
