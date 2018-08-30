import os
import re
import settings

# Unlike the DDS implementation of this, only get b numbers that are correctly arranged 
# in their 4-level file structure

# provide "/" to enumerate all, or be more selective and pass "/5/5..." etc.

valid_dir_names = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'x']


def b_numbers_from_fileshare(start_at):
    b_number_pattern = re.compile('\A(b[0-9ax]{8}).xml\Z')
    mets_root = os.path.join(settings.METS_FILESYSTEM_ROOT, start_at)
    for dirpath, dirnames, filenames in os.walk(mets_root, topdown=True):
        this_dir = os.path.dirname(dirpath)
        if os.path.basename(this_dir) in valid_dir_names:
            for f in filenames:
                m = b_number_pattern.match(f)
                if m:
                    yield m.group(1)


def b_numbers_from_s3(start_at):
    pass
