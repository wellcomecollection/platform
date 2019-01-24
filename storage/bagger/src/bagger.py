"""Creates bagit bags by processing METS files and collecting assets from their various locations.

Can run in mets-only mode, where no bags are created
and no I/O operations happen, other than on METS files.
"""

import sys
import os
import collections
import shutil
import logging
import bagit
import settings
import mets
import identifiers
import bag_assembly
import files
import aws
import tech_md
from xml_help import load_from_disk, load_from_string

logging.basicConfig(
    format="%(process)d - %(threadName)s - %(levelname)s: %(message)s",
    level=logging.INFO,
)
logging.getLogger("bagit").setLevel(logging.ERROR)


def bag_from_identifier(identifier, skip_file_download):
    b_number = identifiers.normalise_b_number(identifier)
    bag_details = bag_assembly.prepare_bag_dir(b_number)
    id_map = collections.OrderedDict()
    mets_path = "{0}{1}.xml".format(bag_details["mets_partial_path"], b_number)

    logging.debug("process METS or anchor file at %s", mets_path)

    tree = load_xml(mets_path)
    root = tree.getroot()
    title = mets.get_title(root)

    logging.info("-> started bagging {0}: {1}".format(b_number, title))
    logging.debug(
        "We will transform xml that involves Preservica, and the tessella namespace"
    )

    # Compare the logic here with the METS Repository
    # This is simpler as we always start with a b number
    # (we don't arrive with the identifier of a particular volume)

    # determine what kind of file this is from the Logical Struct Div
    struct_div = mets.get_logical_struct_div(root)
    struct_type = struct_div.get("TYPE")
    struct_label = struct_div.get("LABEL")

    logging.debug("Found structDiv with TYPE " + struct_type)
    logging.debug("LABEL: " + struct_label)

    root_mets_file = os.path.join(bag_details["directory"], "{0}.xml".format(b_number))

    # There is a huge amount of validation that can be done here.
    # Not just the checksums, but also validating the METS structure,
    # validating that MultipleManifestation anchors and Manifestations
    # agree with each other, etc.

    if mets.is_collection(struct_type):
        logging.debug("This root METS file is an _anchor_ file.")
        logging.debug("It points to multiple manifestations.")
        # probably don't need other transforms as no reference to assets
        # That is, nothing will change if the assets are stored elsewhere
        manifestation_relative_paths = []
        for div in struct_div:
            order = div.get("ORDER", None)
            assert order is not None, "No order attribute"
            assert int(order), "ORDER is not an integer: " + order
            assert int(order) > 0, "ORDER {0} <= 0".format(order)  # can it be 0?
            assert len(div) == 1, "one and only one child element"
            file_pointer_href = mets.get_file_pointer_link(div)
            logging.debug("link to manifestation file: " + file_pointer_href)
            manifestation_relative_paths.append((file_pointer_href, order))

        # TODO - we have stored the order, we could validate that the manifestation files match this order

        logging.debug("writing new anchor file to bag")
        tree.write(root_mets_file, encoding="utf-8", xml_declaration=True)
        aws.save_mets_to_side(b_number, root_mets_file)
        # then go through the linked files _0001 etc
        for rel_path in manifestation_relative_paths:
            full_path = bag_details["mets_partial_path"] + rel_path[0]
            logging.debug("loading manifestation " + full_path)
            logging.debug("ORDER: {0}".format(rel_path[1]))
            mf_tree = load_xml(full_path)
            mf_root = mf_tree.getroot()
            manif_struct_div = mets.get_logical_struct_div(mf_root)
            link_to_anchor = mets.get_file_pointer_link(manif_struct_div)
            logging.debug("{0} should be link back to anchor".format(link_to_anchor))
            process_manifestation(mf_root, bag_details, skip_file_download, id_map)
            # not os separator, this is in the METS; always /
            parts = rel_path[0].split("/")
            manifestation_file = os.path.join(bag_details["directory"], *parts)
            logging.debug("writing manifestation to bag: " + manifestation_file)
            mf_tree.write(manifestation_file, encoding="utf-8", xml_declaration=True)
            aws.save_mets_to_side(b_number, manifestation_file)

    elif mets.is_manifestation(struct_type):
        process_manifestation(root, bag_details, skip_file_download, id_map)
        tree.write(root_mets_file, encoding="utf-8", xml_declaration=True)
        aws.save_mets_to_side(b_number, root_mets_file)

    else:
        raise ValueError("Unknown struct type: " + struct_type)

    aws.save_id_map(b_number, id_map)

    if skip_file_download:
        print(b_number)
        logging.info("Finished {0} without bagging".format(b_number))
        bag_assembly.cleanup_bnumber_files(b_number)
        return None

    bagit.make_bag(bag_details["directory"], get_bag_info(b_number, title))

    upload_location = dispatch_bag(bag_details)

    bag_assembly.cleanup_bnumber_files(b_number)

    logging.debug("Finished bagging {0}".format(b_number))

    return upload_location


def process_manifestation(root, bag_details, skip_file_download, id_map):
    mets.remove_deliverable_unit(root)
    tech_md.remodel_file_technical_metadata(root, id_map)
    mets.remodel_file_section(root)
    assets, alto = mets.get_physical_file_maps(root)
    files.process_assets(root, bag_details, assets, skip_file_download)
    files.process_alto(root, bag_details, alto, skip_file_download)


def load_xml(path):
    if settings.READ_METS_FROM_FILESHARE:
        logging.debug("Reading METS from Windows Fileshare")
        return load_from_disk(settings.METS_FILESYSTEM_ROOT + path)

    logging.debug("Reading METS from S3")
    xml_string = aws.get_mets_xml(path)
    return load_from_string(xml_string)


def dispatch_bag(bag_details):
    # now zip this bag in a way that will be efficient for the archiver
    logging.debug("creating zip file for " + bag_details["b_number"])

    shutil.make_archive(
        bag_details["zip_file_path"][0:-4], "zip", bag_details["directory"]
    )

    logging.debug("uploading " + bag_details["zip_file_name"] + " to S3")

    upload_location = aws.upload(
        bag_details["zip_file_path"], bag_details["zip_file_name"]
    )

    logging.debug("upload completed")

    return upload_location


def get_bag_info(b_number, title):
    bag_info = dict(settings.BAG_INFO)
    bag_info["External-Description"] = title
    bag_info["External-Identifier"] = b_number
    return bag_info


def main():
    if len(sys.argv) == 1:
        print()
        print("USAGE")
        print()
        print("> bagger.py clean")
        print("Delete contents of working directory.")
        print()
        print("> bagger.py b12345678 <bag|no-bag>")
        print("Bag a b number. If 'no-bag', process METS only.")
        print()
        return

    if sys.argv[1] == "clean":
        bag_assembly.clean_working_dir()
        return

    skip_file_download = False
    if len(sys.argv) == 3 and sys.argv[2] == "no-bag":
        logging.debug(
            "skipping copying and bagging operations, will just process METS."
        )
        skip_file_download = True

    identifier = sys.argv[1]
    bag_from_identifier(identifier, skip_file_download)


if __name__ == "__main__":
    main()
