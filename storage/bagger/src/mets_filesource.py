import os
import re
import settings
from pathlib import Path
from s3_keys import get_matching_s3_keys

# Unlike the DDS implementation of this, only get b numbers that are correctly arranged
#  in their 4-level file structure

# provide "/" to enumerate all, or be more selective and pass "/5/5..." etc.

valid_dir_names = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "x"]


def b_numbers_from_fileshare(start_at):
    b_number_pattern = re.compile(r"\A(b[0-9ax]{8}).xml\Z")
    mets_root = os.path.join(settings.METS_FILESYSTEM_ROOT, start_at)
    for dirpath, _, filenames in os.walk(mets_root, topdown=True):
        this_dir = os.path.dirname(dirpath)
        if os.path.basename(this_dir) in valid_dir_names:
            for f in filenames:
                m = b_number_pattern.match(f)
                if m:
                    yield m.group(1)


def b_numbers_from_s3(filter=""):
    # We don't just want to enumerate all the keys in the bucket, as the majority of
    # keys will be for ALTO files (one per image), with multiple manifestations as well.

    # options:
    # 1: Issue a prefix query for each level 4 directory, from mets/0/0/0/0 to mets/x/9/9/9
    # - this will yield a lot of missing keys, involves 11000 queries
    # 2: make a prefix query for mets/ and just keep iterating. Can we skip ahead when we find ALTO?
    #  underlying AWS API query has page size of 1000, so will involve 20,000 queries to S3 if we do that
    # 3. use the /nets_only prefix. This "folder" omits the ALTO files, so will only contain
    # METS for b numbers and multiple manifestations. Although we don't need the MMs, they will only make
    # up a third or so of the total keys, and we can skip them.

    # 3 seems most efficient
    prefix = settings.METS_ONLY_ROOT_PREFIX
    b_number_pattern = re.compile(r"\A" + prefix + r"[0-9ax/]*/(b[0-9ax]{8}).xml\Z")
    for key in get_matching_s3_keys(
        bucket=settings.METS_BUCKET_NAME, prefix=prefix + filter
    ):
        m = b_number_pattern.match(key)
        if m:
            yield m.group(1)


def bnumber_generator(filter_expression):
    source_list = Path(filter_expression)
    if source_list.is_file():
        with open(filter_expression) as f:
            bnumbers = f.readlines()
        return [b.strip() for b in bnumbers if not b.strip() == ""]
    elif filter_expression.startswith("b"):
        return (b for b in [filter_expression])
    else:
        return b_numbers_from_s3(filter_expression)
