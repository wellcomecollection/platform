import sys
import os
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

logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.INFO)


def main():
    if sys.argv[1] == "clean":
        bag_assembly.clean_working_dir()
        return

    b_number = identifiers.normalise_b_number(sys.argv[1])
    bag_info = bag_assembly.prepare_bag_dir(b_number)
    mets_path = "{0}{1}.xml".format(bag_info["mets_partial_path"], b_number)
    logging.info("process METS or anchor file at %s", mets_path)
    tree = load_xml(mets_path)
    root = tree.getroot()

    logging.info("We will transform xml that involves Preservica, and the tessella namespace")

    # Compare the logic here with the METS Repository
    # This is simpler as we always start with a b number
    # (we don't arrive with the identifier of a particular volume)

    # determine what kind of file this is from the Logical Struct Div
    struct_div = mets.get_logical_struct_div(root)
    struct_type = struct_div.get("TYPE")
    struct_label = struct_div.get("LABEL")
    logging.info("Found structDiv with TYPE " + struct_type)
    logging.info("LABEL: " + struct_label)
    root_mets_file = os.path.join(bag_info["directory"], "{0}.xml".format(b_number))

    # There is a huge amount of validation that can be done here.
    # Not just the checksums, but also validating the METS structure,
    # validating that MultipleManifestation anchors and Manifestations
    # agree with each other, etc.

    if mets.is_collection(struct_type):
        logging.info("This root METS file is an _anchor_ file.")
        logging.info("It points to multiple manifestations.")
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
            logging.info("link to manifestation file: " + file_pointer_href)
            manifestation_relative_paths.append((file_pointer_href, order))

        # TODO - we have stored the order, we could validate that the manifestation files match this order

        logging.info("writing new anchor file to bag")
        tree.write(root_mets_file, encoding="utf-8", xml_declaration=True)
        # then go through the linked files _0001 etc
        for rel_path in manifestation_relative_paths:
            full_path = bag_info["mets_partial_path"] + rel_path[0]
            logging.info("loading manifestation " + full_path)
            logging.info("ORDER: {0}".format(rel_path[1]))
            mf_tree = load_xml(full_path)
            mf_root = mf_tree.getroot()
            manif_struct_div = mets.get_logical_struct_div(mf_root)
            link_to_anchor = mets.get_file_pointer_link(manif_struct_div)
            logging.info("{0} should be link back to anchor".format(link_to_anchor))
            process_manifestation(mf_root, bag_info)
            # not os separator, this is in the METS; always /
            parts = rel_path[0].split("/")
            manifestation_file = os.path.join(bag_info["directory"], *parts)
            logging.info("writing manifestation to bag: " + manifestation_file)
            mf_tree.write(manifestation_file, encoding="utf-8", xml_declaration=True)

    elif mets.is_manifestation(struct_type):
        process_manifestation(root, bag_info)
        tree.write(root_mets_file, encoding="utf-8", xml_declaration=True)

    else:
        raise ValueError("Unknown struct type: " + struct_type)

    bagit.make_bag(bag_info["directory"], {"Contact-Name": "Tom"})

    dispatch_bag(bag_info)
    logging.info("Finished " + b_number)


def process_manifestation(root, bag_info):
    mets.remove_deliverable_unit(root)
    tech_md.remodel_file_technical_metadata(root)
    mets.remodel_file_section(root)
    assets, alto = mets.get_physical_file_maps(root)
    files.collect_assets(root, bag_info, assets)
    files.collect_alto(root, bag_info, alto)


def load_xml(path):
    if settings.METS_FILESYSTEM_ROOT:
        logging.info("Reading METS from Windows Fileshare")
        return load_from_disk(settings.METS_FILESYSTEM_ROOT + path)

    logging.info("Reading METS from S3")
    xml_string = aws.get_mets_xml(path)
    return load_from_string(xml_string)


def dispatch_bag(bag_info):
    # now zip this bag in a way that will be efficient for the archiver
    logging.info("creating zip file for " + bag_info["b_number"])
    shutil.make_archive(bag_info["zip_file_path"]
                        [0:-4], 'zip', bag_info["directory"])
    logging.info("uploading " + bag_info["zip_file_name"] + " to S3")
    aws.upload(bag_info["zip_file_path"], bag_info["zip_file_name"])
    logging.info("upload completed")


if __name__ == "__main__":
    main()
