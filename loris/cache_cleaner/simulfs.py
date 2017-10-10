# -*- encoding: utf-8 -*-
"""
This is an in-memory representation of a hierarchical filesystem,
for use when working with EFS.

EFS is using NFS (network filesystem) under the hood, which causes OS calls
to be unusually slow.  This allows us to walk a tree once, then record all
the information about it in-memory and determine which files to delete without
going back to the OS.

It makes the following simplifying assumptions:

*   Directories take up no space.
*   Everything in a hierarchy is a file or a directory, nothing else.
*   The maximum depth of the hierarchy is fairly small (well below Python's
    max recursion limit)

"""

import errno
import os
import sys


def stat_f(path):
    return os.stat(path)


def remove_f(path):
    """Remove a file."""
    if os.environ.get('X-RUN-CACHE-CLEANER'):
        try:
            os.unlink(path)
        except FileNotFoundError:
            pass


def remove_d(path):
    """Remove a directory."""
    if os.environ.get('X-RUN-CACHE-CLEANER'):
        os.rmdir(path)


class File:
    """
    Represents a single file on the filesystem.
    """
    def __init__(self, path, parent=None):
        self.path = path
        self.stat = stat_f(path)
        self.parent = parent

    def __repr__(self):
        return f'{type(self).__name__}(path={self.path!r})'

    @property
    def last_access_time(self):
        return self.stat.st_atime

    @property
    def size(self):
        """
        Returns the size of the file in bytes.
        """
        return self.stat.st_size

    def delete(self):
        try:
            remove_f(self.path)
        except PermissionError as err:
            print(f'Failed to delete {self.path}: {err}', file=sys.stderr)
        else:
            print(f'Deleted file {self.path}')
            self.parent.children.remove(self)
            if self.parent.is_empty:
                self.parent.remove()


class Directory:
    """
    Represents a directory in the filesystem.
    """
    def __init__(self, path, parent=None):
        self.path = path
        self.parent = parent
        self.children = []
        self.populate()

    def __repr__(self):
        return f'{type(self).__name__}(path={self.path!r})'

    @property
    def is_empty(self):
        return len(self.children) == 0

    def populate(self):
        for item in os.listdir(self.path):
            path = os.path.join(self.path, item)
            if os.path.isfile(path):
                self.children.append(File(path=path, parent=self))
            elif os.path.isdir(path):
                d = Directory(path=path, parent=self)
                self.children.append(d)
            else:
                assert False, path

    @property
    def files(self):
        for child in list(self.children):
            if isinstance(child, File):
                yield child
            else:
                yield from list(child.files)

    @property
    def size(self):
        """
        Returns the size of the directory and files beneath it in bytes.

        Note: this makes a simplifying assumption that directories do not
        take up any size.  This isn't strictly true; running ``du`` on an
        empty directory reveals that it takes up a 4K block on the filesystem,
        but that should be small compared to the size of our cache.
        """
        return sum(child.size for child in self.children)

    def remove(self):
        try:
            remove_d(self.path)
        except OSError as err:
            if err.errno == errno.ENOTEMPTY:
                pass
            else:
                raise
        else:
            print(f'Deleted empty directory {self.path}')
            self.parent.children.remove(self)
            if self.parent.is_empty:
                self.parent.remove()


class SimulatedFS(Directory):
    """
    Simulated filesystem.  This is the starting point when using this module.
    """
    def remove(self):
        pass
