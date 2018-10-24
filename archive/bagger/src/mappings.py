# to be added to...
# bagger will throw an exception if the format in METS is not a key in this dictionary.
PRONOM = {
    "JP2 (JPEG 2000 part 1)": "x-fmt/392",
    "JPEG2000": "x-fmt/392",
    "MPEG 1/2 Audio Layer 3": "fmt/134",
    "MPEG-2 Video Format": "x-fmt/386",
    "MPEG-4 Media File": "fmt/199",
    "Acrobat PDF 1.0 - Portable Document Format 1.0": "fmt/14",
    "Acrobat PDF 1.1 - Portable Document Format 1.1": "fmt/15",
    "Acrobat PDF 1.2 - Portable Document Format 1.2": "fmt/16",
    "Acrobat PDF 1.3 - Portable Document Format 1.3": "fmt/17",
    "Acrobat PDF 1.4 - Portable Document Format 1.4": "fmt/18",
    "Acrobat PDF 1.5 - Portable Document Format 1.5": "fmt/19",
    "Acrobat PDF 1.6 - Portable Document Format 1.6": "fmt/20",
    "Acrobat PDF 1.7 - Portable Document Format 1.7": "fmt/276",
    "Acrobat PDF/X - Portable Document Format - Exchange 1a:2001": "fmt/157",
    "Acrobat PDF/X - Portable Document Format - Exchange PDF/X-4": "fmt/488",
    "Extensible Markup Language 1.0": "fmt/101",
}

FOR_CONSIDERATION = {
    "JPEG2000 / Macintosh PICT Image 1.0": "x-fmt/392",
    "JPEG File Interchange Format 1.01": "x-fmt/392",
}

# map of tessella names to premis names
# also, presence in here means we want it in new version
SIGNIFICANT_PROPERTIES = {
    "Image Width": "ImageWidth",
    "Image Height": "ImageHeight",
    "Bitrate (kbps)": "Bitrate",
    "Length In Seconds": "Duration",
    "Number of Pages": "PageNumber",  # this is a property of PDFs
}

# Do not transform any of these file properties.
# If a file property is encountered that isn't in either list, an error will be raised.
IGNORED_PROPERTIES = [
    "Creation Date",
    "Number of Images",
    "Bits Per Sample",
    "Samples Per Pixel",
]

IGNORED_TECHMD_FILENAMES = ["thumbs.db"]

CHECKSUM_ALGORITHMS = {"2": "SHA-1"}
