#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Purge cache based on age of files and the size of the cache.

Usage:
  cache_cleaner.py --path=<PATH> --max-age=<MAX_AGE> --max-size=<MAX_SIZE> [--force]
  cache_cleaner.py -h | --help

Options:
  -h --help                 Show this screen.
  --path=<PATH>             Path of the cache to clean.
  --max-age=<MAX_AGE>       Delete files that are older than MAX_AGE days.
  --max-size=<MAX_SIZE>     Delete files until the cache size is less than MAX_SIZE Kbytes.
                            Supports human-friendly options: 500M, 2G, 1T.
  --force                   Actually delete the files.  Otherwise runs in a "dry run" mode,
                            printing which files would be deleted without actually deleting.

"""

import os
import re
import time

import docopt

import simulfs


def parse_max_cache_size_arg(value):
    m = re.match(r'([0-9]+)(K|M|G|T)', value)
    if m is not None:
        size = int(m.group(1))
        suffix = m.group(2)
        if suffix == 'K':
            return size * 1024
        elif suffix == 'M':
            return size * 1024 * 1024
        elif suffix == 'G':
            return size * 1024 * 1024 * 1024
        elif suffix == 'T':
            return size * 1024 * 1024 * 1024 * 1024
    return int(value)


def main():
    args = docopt.docopt(__doc__)

    now = time.time()
    max_age = int(args['--max-age']) * 24 * 60 * 60
    cache_path = args['--path']
    max_cache_size = parse_max_cache_size_arg(args['--max-size'])

    force = bool(args['--force'])
    if force:
        os.environ['X-RUN-CACHE-CLEANER'] = 'True'

    print(f'*** Walking filesystem for {cache_path}')
    fs = simulfs.SimulatedFS(cache_path)

    # Start by deleting files that are older than a certain age.
    print(f'*** Deleting files that are more than {max_age} seconds old')
    for f in fs.files:
        if now - f.last_access_time > max_age:
            f.delete()

    # If the size of the system is still too large, continue deleting
    # files until we're under the limit.
    files_by_size = sorted(fs.files, key=lambda f: f.last_access_time)
    print(f'*** Deleting files to bring the cache under {max_cache_size} Kbytes')
    while fs.size > max_cache_size:
        next_to_delete = files_by_size.pop(0)
        next_to_delete.delete()

    print(f'*** Cache clearing complete')


if __name__ == "__main__":
    main()
