from requests import get
import settings


def get_package_file_modified(bnumber):
    url = settings.DDS_PACKAGE_FILEINFO.format(bnumber)
    info = get(url).json()
    if info.get("Exists", False):
        return info["LastWriteTime"]
    return None


def notify_dds_goobi_call(bnumber):
    url = settings.DDS_GOOBI_NOTIFICATION.format(bnumber)
    return get(url).json()
