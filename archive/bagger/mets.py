# Borrowed from Dds.Dashboard, LogicalStructDiv impl

collection_types = [
    "MultipleManifestation",
    "Periodical",
    "PeriodicalVolume"
]

manifestation_types = [
    "Monograph",
    "Archive",
    "Artwork",
    "Manuscript",
    "PeriodicalIssue",
    "Video",
    "Transcript",
    "MultipleVolume",
    "MultipleCopy",
    "MultipleVolumeMultipleCopy",
    "Audio"
]

def is_collection(struct_type):
    return struct_type in collection_types

def is_manifestation(struct_type):
    return struct_type in manifestation_types