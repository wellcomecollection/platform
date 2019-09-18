#!/usr/bin/env python
# -*- encoding: utf-8

from configobj import ConfigObj
import os
import pathlib
import sys

sys.path.insert(
    0, str(pathlib.Path(__file__).parent.parent / "loris")
)
from user_commands import create_default_files_and_directories


if __name__ == "__main__":
    config = ConfigObj(
        os.environ["LORIS_CONF_FILE"],
        unrepr=True,
        interpolation=False
    )
    create_default_files_and_directories(config)
