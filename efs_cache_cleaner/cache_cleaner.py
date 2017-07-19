#!/usr/bin/env python
# -*- encoding: utf-8 -*-
import os
import subprocess

import time

import sys


def delete(path):
    print(path)
    try:
        os.unlink(path)
    except PermissionError as err:
        print(f"Failed to delete {path}: {err}", file=sys.stderr)


def delete_directory_if_empty(path):
    try:
        os.rmdir(path)
        print(path)
    except OSError:
        # TODO check that it fails because the directory is not empty
        pass


def get_directory_size(path):
    result = subprocess.check_output(["du", "-skH", path])
    return int(result.split()[0])


def main():
    now = time.time()
    max_age = 1 * 24 * 60 * 60
    tmp = "/tmp"
    for root, _, filenames in os.walk(tmp):
        for f in filenames:
            path = os.path.join(root, f)
            last_access_time = os.stat(path).st_atime
            if now - last_access_time > max_age:
                delete(path)

    size = get_directory_size(tmp)
    print(size)
    if size > 10:
        pass

    for root, dirnames, _ in os.walk(tmp):
        for directory in dirnames:
            path = os.path.join(root, directory)
            delete_directory_if_empty(path)


if __name__ == "__main__":
    main()
