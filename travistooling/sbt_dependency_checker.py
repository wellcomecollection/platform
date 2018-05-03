# -*- encoding: utf-8

import functools
import os
import subprocess


@functools.lru_cache()
def does_path_depend_on_library(path, library_name):
    """Given a path to an sbt app and the name of one of our common libraries,
    does this app depend on that library?

    It uses a somewhat crude heuristic (grep) rather than looking at the build
    configuration.  Parsing build.sbt is icky, and trying to get sbt to give
    up a dependency tree in a sensible format is slow and also icky.

    >>> does_path_depend_on_library('catalogue_api/api', 'messaging')
    False

    >>> does_path_depend_on_library('catalogue_api/api', 'elasticsearch')
    True

    """
    try:
        subprocess.check_call([
            'grep', '--recursive',
            'import uk.ac.wellcome.%s' % library_name,
            os.path.join(path, 'src', 'main', 'scala')
        ], stdout=subprocess.DEVNULL)
    except subprocess.CalledProcessError:
        return False
    else:
        return True
