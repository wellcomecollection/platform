import os
import shutil
import settings


def clean_working_dir():
    for f in os.listdir(settings.WORKING_DIRECTORY):
        path = os.path.join(settings.WORKING_DIRECTORY, f)
        if os.path.isfile(path):
            os.remove(path)
        elif os.path.isdir(path):
            shutil.rmtree(path)


def prepare_bag_dir(b_number):

    zip_file_name = "{0}.zip".format(b_number)
    bag_info = {
        "b_number": b_number,
        "directory": os.path.join(settings.WORKING_DIRECTORY, b_number),
        "zip_file_name": zip_file_name,
        "zip_file_path": os.path.join(settings.WORKING_DIRECTORY, zip_file_name),
        "mets_partial_path": get_mets_partial_path(b_number)
    }
    shutil.rmtree(bag_info["directory"], ignore_errors=True)
    try:
        os.remove(bag_info["zip_file_path"])
    except OSError:
        pass

    if os.path.isdir(bag_info["directory"]):
        raise FileExistsError("Unable to start with clean bag directory")
    if os.path.isfile(bag_info["zip_file_path"]):
        raise FileExistsError("Unable to remove existing zipped bag")

    # bagit makes the data directory itself
    os.makedirs(os.path.join(bag_info["directory"], "objects"))

    return bag_info


def get_mets_partial_path(b_number):
    separator = get_separator()
    pathparts = separator.join(b_number[9:4:-1])
    return "{0}{1}{2}".format(settings.METS_ROOT_PREFIX, pathparts, separator)
    # return "{0}{1}{2}{3}.xml".format(settings.METS_ROOT_PREFIX, pathparts, separator, b_number)


def get_separator():
    # Not necessarily the OS we're running on!
    if settings.METS_FILESYSTEM_ROOT:
        return "\\"
    return "/"


def ensure_directory(destination):
    destination_dir = os.path.dirname(destination)
    if not os.path.exists(destination_dir):
        os.makedirs(destination_dir)  # TODO: beware race condition if threaded
