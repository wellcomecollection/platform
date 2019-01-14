import settings


def analyse_origin(origin, pres_uuid):
    # the origin will be one of three expected locations
    #   1. The Preservica Bucket
    #   2. The DLCS storage bucket
    #   3. wellcomelibrary.org
    # these rules aren't very robust and are very specific to current content
    origin_info = {
        "bucket_name": None,
        "bucket_key": None,
        "alt_key": None,
        "web_url": None,
    }
    if origin is None:
        # The DLCS doesn't know about this image. It might still be in the
        # Preservica bucket, so we can try that. Otherwise, the migrator will
        # have to try to get it from wl.org.
        origin_info["web_url"] = "{0}{1}".format(settings.DDS_ASSET_PREFIX, pres_uuid)
        origin_info["bucket_name"] = settings.CURRENT_PRESERVATION_BUCKET
        origin_info["bucket_key"] = pres_uuid
        return origin_info

    if origin.startswith(settings.DDS_ASSET_PREFIX):
        # check if file is in settings.DLCS_SOURCE_BUCKET
        # if so, return that bucket's details
        origin_info["web_url"] = origin
        origin_info["bucket_name"] = settings.DLCS_SOURCE_BUCKET
        origin_info["bucket_key"] = "{0}/{1}/{2}".format(
            settings.DLCS_CUSTOMER_ID, settings.DLCS_SPACE, pres_uuid
        )
        # messy, a small %age of DLCS JP2s have a file extension
        origin_info["alt_key"] = origin_info["bucket_key"] + ".jp2"

        return origin_info

    parts = origin.split("/")
    if parts[3] == settings.CURRENT_PRESERVATION_BUCKET:
        origin_info["bucket_name"] = settings.CURRENT_PRESERVATION_BUCKET
        origin_info["bucket_key"] = parts[4]

    return origin_info
