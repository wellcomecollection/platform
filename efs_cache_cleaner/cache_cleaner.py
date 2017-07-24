#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Purge cache based on age of files and the size of the cache.

Usage:
  cache_cleaner.py --path=<PATH> --max-age=<MAX_AGE> --max-size=<MAX_SIZE> [--force]
  cache_cleaner.py -h | --help

Options:
  -h --help                Show this screen.
  --path=<PATH>            Path of the cache to clean.
  --max-age=<MAX_AGE>      Delete files that are older than MAX_AGE days.
  --max-size=<MAX_SIZE>    Delete files until the cache size is less than MAX_SIZE Kbytes.
  --force                  Actually delete the files.  Otherwise runs in a "dry run" mode,
                           printing which files would be deleted without actually deleting.

"""

import os
import time

import docopt

import simulfs


def main():
    args = docopt.docopt(__doc__)

    now = time.time()
    max_age = int(args['--max-age']) * 24 * 60 * 60
    cache_path = args['--path']
    max_cache_size = int(args['--max-size'])
    force = bool(args['--force'])
    if force:
        os.environ['X-RUN-CACHE-CLEANER'] = 'True'

    fs = simulfs.SimulatedFS(cache_path)

    # Start by deleting files that are older than a certain age.
    print(f'*** Deleting files that are more than {max_age} seconds old')
    for f in fs.files:
        if now - f.last_access_time > max_age:
            f.delete()

    # If the size of the system is still too large, continue deleting
    # files until we're under the limit.
    files_by_size = sorted(fs.files, key=lambda f: f.last_access_time)
    print(f'*** Deleting files to bring the cache under size {max_cache_size}')
    while fs.size > max_cache_size:
        next_to_delete = files_by_size.pop(0)
        next_to_delete.delete()

    print(f'*** Cache clearing complete')


if __name__ == "__main__":
    main()
