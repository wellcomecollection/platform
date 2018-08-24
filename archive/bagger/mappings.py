
# to be added to...
# bagger will throw an exception if the format in METS is not a key in this dictionary.
PRONOM = {
    "JP2 (JPEG 2000 part 1)": "x-fmt/392",
    "MPEG-2 Video Format": "x-fmt/386",
    "Acrobat PDF 1.4 - Portable Document Format 1.4": "fmt/18"
}

# map of tessella names to premis names
# also, presence in here means we want it in new version
SIGNIFICANT_PROPS = {
    "Image Width": "ImageWidth",
    "Image Height": "ImageHeight",
    "Bitrate (kbps)": "Bitrate",
    "Length In Seconds": "Duration",
    "Number of Pages": "PageNumber"  # this is a property of PDFs
}

# Ignored properties observed so far:

# "Creation Date"
# "Number of Images"
# "Bits Per Sample"
# "Samples Per Pixel"
