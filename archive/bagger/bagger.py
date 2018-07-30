import sys
import os
import shutil
import logging
import xml.etree.ElementTree as ET
import boto3
import bagit
import requests
import settings
import mets
import dlcs

logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.INFO)

# digiflow is made up
namespaces = {
    'mets': 'http://www.loc.gov/METS/',
    'mods': 'http://www.loc.gov/mods/v3',
    'tessella': 'http://www.tessella.com/transfer',
    'dv': 'http://dfg-viewer.de/',
    'xlink': 'http://www.w3.org/1999/xlink',
    'digiflow': 'http://wellcomecollection.org/ns/digiflow'
}

for prefix, namespace in namespaces.items():
    ET.register_namespace(prefix, namespace)

boto_session = None


def main():

    if sys.argv[1] == "clean":
        clean_working_dir()
        return

    b_number = normalise_b_number(sys.argv[1])
    bag_info = prepare_bag_dir(b_number)
    mets_path = "{0}{1}.xml".format(bag_info["mets_partial_path"], b_number)
    logging.info("process METS or anchor file at %s", mets_path)
    tree = load_xml(mets_path)
    root = tree.getroot()

    # TODO - is this an anchor file or a regular METS?

    # mets:dmdSec * 2
    # Leave the METS header and dmdSecs the same
    # ----

    logging.info(
        "We will transform xml that involves Preservica, and the tessella namespace")

    # Compare the logic here with the METS Repository
    # This is simpler as we always start with a b number
    # (we don't arrive with the identifier of a particular volume)

    # determine what kind of file this is from the Logical Struct Div
    struct_div = get_logical_struct_div(root)
    struct_type = struct_div.get("TYPE")
    struct_label = struct_div.get("LABEL")
    logging.info("Found structDiv with TYPE " + struct_type)
    logging.info("LABEL: " + struct_label)
    root_mets_file = os.path.join(
        bag_info["directory"], "{0}.xml".format(b_number))

    # There is a huge amount of validation that can be done here.
    # Not just the checksums, but also validatig the METS structure,
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
            assert int(order) > 0, "ORDER {0} <= 0".format(
                order)  # can it be 0?
            assert len(div) == 1, "one and only one child element"
            file_pointer_href = ensure_file_pointers(div)
            logging.info("link to manifestation file: " + file_pointer_href)
            manifestation_relative_paths.append((file_pointer_href, order))

        # TODO - we have stored the order, we could validate that the manifestation
        # files match this order

        logging.info("writing new anchor file to bag")
        tree.write(root_mets_file, encoding="utf-8")
        # then go through the linked files _0001 etc
        for rel_path in manifestation_relative_paths:
            full_path = bag_info["mets_partial_path"] + rel_path[0]
            logging.info("loading manifestation " + full_path)
            logging.info("ORDER: {0}".format(rel_path[1]))
            mf_tree = load_xml(full_path)
            mf_root = mf_tree.getroot()
            manif_struct_div = get_logical_struct_div(mf_root)
            link_to_anchor = ensure_file_pointers(manif_struct_div)
            logging.info(
                "{0} should be link back to anchor".format(link_to_anchor))
            process_manifestation(mf_root, bag_info)
            # not separator, this is in the METS
            parts = rel_path[0].split("/")
            manifestation_file = os.path.join(bag_info["directory"], *parts)
            logging.info("writing manifestation to bag: " + manifestation_file)
            mf_tree.write(manifestation_file, encoding="utf-8")

    elif mets.is_manifestation(struct_type):
        process_manifestation(root, bag_info)
        tree.write(root_mets_file, encoding="utf-8")

    else:
        raise ValueError("Unknown struct type: " + struct_type)

    bagit.make_bag(bag_info["directory"], {"Contact-Name": "Tom"})

    dispatch_bag(bag_info)
    logging.info("Finished " + b_number)


def get_logical_struct_div(root):

    logical_struct_map = root.find(
        "./mets:structMap[@TYPE='LOGICAL']", namespaces)
    assert len(logical_struct_map) == 1, "Expect one element in logical structMap"
    return logical_struct_map[0]


def ensure_file_pointers(struct_div):

    tag = struct_div[0].tag
    expected = get_expanded_form("mets", "mptr")
    assert tag == expected, "expected {0}, found {1}".format(expected, tag)
    assert struct_div[0].get("LOCTYPE") == "URL", "Pointer is not URL"
    xlink_href = get_expanded_form("xlink", "href")
    link = struct_div[0].get(xlink_href)
    if not link.endswith(".xml"):
        logging.info("Adding .xml to mets file pointer")
        link = link + ".xml"
        struct_div[0].set(xlink_href, link)

    logging.info("obtained link from pointer: " + link)
    return link


def process_manifestation(root, bag_info):

    # lots of validation work to do in all of these!!!
    remodel_deliverable_unit(root)
    remodel_file_technical_metadata(root)
    remodel_file_section(root)
    assets, alto = get_physical_file_maps(root)
    collect_assets(root, bag_info, assets)
    collect_alto(root, bag_info, alto)


def load_xml(path):
    if settings.METS_FILESYSTEM_ROOT:
        logging.info("Reading METS from Windows Fileshare")
        return ET.parse(settings.METS_FILESYSTEM_ROOT + path)

    logging.info("Reading METS from S3")
    s3 = get_boto_session().resource("s3")
    obj = s3.Object(settings.METS_BUCKET_NAME, path)
    xml_string = obj.get()["Body"].read().decode("utf-8")
    root = ET.fromstring(xml_string)
    return ET.ElementTree(root)


def get_boto_session():
    global boto_session
    if boto_session is None:
        boto_session = boto3.Session(
            aws_access_key_id=settings.AWS_PUBLIC_KEY,
            aws_secret_access_key=settings.AWS_SECRET_KEY,
            region_name=settings.AWS_REGION
        )
    return boto_session


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


def dispatch_bag(bag_info):

    # now zip this bag in a way that will be efficient for the archiver
    logging.info("creating zip file for " + bag_info["b_number"])
    shutil.make_archive(bag_info["zip_file_path"]
                        [0:-4], 'zip', bag_info["directory"])
    logging.info("uploading " + bag_info["zip_file_name"] + " to S3")
    s3 = get_boto_session().client("s3")
    s3.upload_file(bag_info["zip_file_path"],
                   settings.DROP_BUCKET_NAME, bag_info["zip_file_name"])
    logging.info("upload completed")


def remodel_deliverable_unit(root):
    logging.info("Looking at DeliverableUnit")
    x_path = ".//mets:xmlData[tessella:DeliverableUnit]"
    du_xmldata = root.findall(x_path, namespaces)
    if len(du_xmldata) != 1:
        raise KeyError("Expecting one DeliverableUnit XML block")

    # Where does the PurposeName come from?
    # the ID comes from Preservica (?) and can be dropped?
    new_deliverablue_unit = make_child_with_whitespace(
        du_xmldata[0], get_expanded_form("digiflow", "DeliverableUnit"))
    purpose_name_element = make_child_with_whitespace(
        new_deliverablue_unit, get_expanded_form("digiflow", "PurposeName"))
    purpose_name_element.text = du_xmldata[0][0].find(
        "tessella:PurposeName", namespaces).text
    remove_first_child(du_xmldata[0])


def remodel_file_technical_metadata(root):
    logging.info("transforming Tessella techMD")
    x_path = ".//mets:xmlData[tessella:File]"
    tessella_file_xmldata = root.findall(x_path, namespaces)
    for xmldata in tessella_file_xmldata:
        tessella_file = xmldata[0]
        new_file = make_child_with_whitespace(
            xmldata, get_expanded_form("digiflow", "File"))
        carried_over_properties = [
            "FileName",
            "Folder",
            "FormatName",
            "FileSize",
            "Checksum",
            "ChecksumAlgorithmRef"
        ]
        for prop in carried_over_properties:
            copy_simple_value_child(tessella_file, "tessella:" + prop,
                                    new_file, get_expanded_form("digiflow", prop))
        # we'll remove this one later
        copy_simple_value_child(
            tessella_file, "tessella:ID", new_file,
            get_expanded_form("digiflow", "preservicaGuid"))

        remove_first_child(xmldata)
        file_properties = tessella_file.findall(
            "tessella:FileProperty", namespaces)
        to_copy = ["Image Height", "Image Width"]  # more to come
        for file_property in file_properties:
            name = file_property.find(
                "tessella:FilePropertyName", namespaces).text.strip()
            if name in to_copy:
                value = file_property.find(
                    "tessella:Value", namespaces).text.strip()
                new_prop = make_child_with_whitespace(
                    new_file, get_expanded_form("digiflow", "FileProperty"))
                make_child_with_whitespace(
                    new_prop, get_expanded_form("digiflow", "FilePropertyName")).text = name
                make_child_with_whitespace(
                    new_prop, get_expanded_form("digiflow", "Value")).text = value


def remodel_file_section(root):
    logging.info("transforming file section")
    sdb_file_group = root.find(
        "./mets:fileSec/mets:fileGrp[@USE='SDB']", namespaces)
    sdb_file_group.set('USE', 'BAGGER')
    for sdb_file in sdb_file_group:
        sdb_file_id = sdb_file.get("ID")
        bag_file_id = sdb_file_id.replace("_SDB", "_BAG")
        if sdb_file_id == bag_file_id:
            # OK, but what is the strategy here...?
            # This will be OK for experiments
            raise ValueError("SDB ID didn't contain '_SDB'")
        sdb_file.set("ID", bag_file_id)

        # find pointers to this file
        xpath = ".//mets:fptr[@FILEID='{0}']".format(sdb_file_id)
        file_pointers = root.findall(xpath, namespaces)
        for file_pointer in file_pointers:
            file_pointer.set("FILEID", bag_file_id)


def collect_alto(root, bag_info, alto):

    # TODO use the alto map to verify
    logging.info("Collecting ALTO for " + bag_info["b_number"])
    alto_file_group = root.find(
        "./mets:fileSec/mets:fileGrp[@USE='ALTO']", namespaces)

    if alto_file_group is None:
        logging.info("No ALTO for " + bag_info["b_number"])
        return

    source_bucket = None

    if not settings.METS_FILESYSTEM_ROOT:
        s3 = get_boto_session().resource("s3")
        source_bucket = s3.Bucket(settings.METS_BUCKET_NAME)

    for file_element in alto_file_group:
        locator = file_element[0]
        current_location = locator.get(get_expanded_form("xlink", "href"))
        # for now, preserve the folder names in the current METS
        desired_relative_location = "alto/{0}".format(current_location)
        locator.set(get_expanded_form("xlink", "href"),
                    desired_relative_location)
        logging.info("updated ALTO path in METS to " +
                     desired_relative_location)
        alto_path_parts = desired_relative_location.split("/")
        # the local temp assembly area
        destination = os.path.join(bag_info["directory"], *alto_path_parts)
        ensure_directory(destination)

        if settings.METS_FILESYSTEM_ROOT:
            # Not likely to be used much, only for running against Windows file share
            partial_path_parts = bag_info["mets_partial_path"].split(
                get_separator()) + alto_path_parts
            source = os.path.join(
                settings.METS_FILESYSTEM_ROOT, *partial_path_parts)
            logging.info("Copying alto from {0} to {1}".format(
                source, destination))
            shutil.copyfile(source, destination)
        else:
            source = bag_info["mets_partial_path"] + current_location
            logging.info("Downloading S3 ALTO from {0} to {1}".format(
                source, destination))
            source_bucket.download_file(source, destination)


def collect_assets(root, bag_info, assets):

    logging.info("Collecting assets for " + bag_info["b_number"])

    s3 = get_boto_session().resource("s3")
    chunk_size = 1024*1024

    asset_file_group = root.find(
        "./mets:fileSec/mets:fileGrp[@USE='BAGGER']", namespaces)
    for file_element in asset_file_group:
        locator = file_element[0]
        current_location = locator.get(get_expanded_form("xlink", "href"))
        desired_relative_location = "objects/{0}".format(current_location)
        locator.set(get_expanded_form("xlink", "href"),
                    desired_relative_location)
        logging.info("updated asset path in METS to " +
                     desired_relative_location)
        asset_path_parts = desired_relative_location.split("/")
        # the local temp assembly area
        destination = os.path.join(bag_info["directory"], *asset_path_parts)
        ensure_directory(destination)

        summary = assets[file_element.get("ID")]
        tech_md = summary["tech_md"]
        checksum_1 = file_element.get("CHECKSUM")
        checksum_2 = tech_md["Checksum"]
        assert checksum_1 == checksum_2, "Checksums don't match"
        preservica_guid = tech_md["preservicaGuid"]
        logging.info(
            "Need to determine where to get {0} from.".format(preservica_guid))
        image_info = dlcs.get_image(preservica_guid)
        origin = image_info["origin"]
        logging.info("DLCS reports origin " + origin)
        # if the origin is wellcomelibrary.org, the object is LIKELY to be in the DLCS's
        # storage bucket. So we should try that first, then fall back to the wellcomelibrary
        # origin (using the creds) if for whatever reason it isn't in the DLCS bucket.
        origin_info = analyse_origin(origin)
        bucket_name = origin_info["bucket_name"]
        if bucket_name is not None:
            source_bucket = s3.Bucket(bucket_name)
            bucket_key = origin_info["bucket_key"]
            logging.info("Downloading object from bucket {0}/{1} to {2}".format(
                bucket_name, bucket_key, destination))
            source_bucket.download_file(bucket_key, destination)

        elif origin_info["web_url"] is not None:
            user, password = settings.DDS_API_KEY, settings.DDS_API_SECRET
            resp = requests.get(origin_info["web_url"], auth=(user, password), stream=True)
            if resp.status_code == 200:
                with open(destination, 'wb') as f:
                    for chunk in resp.iter_content(chunk_size):
                        f.write(chunk)


        else:
            logging.error("No asset present!")

        logging.info("doing checksums on " + destination)


def analyse_origin(origin):
    # the origin will be one of three expected locations
    #   1. The Preservica Bucket
    #   2. The DLCS storage bucket
    #   3. wellcomelibrary.org
    # these rules aren't very robust and are very specific to current content
    origin_info = {
        "bucket_name": None,
        "bucket_key": None,
        "web_url": None
    }
    if origin.startswith("https://wellcomelibrary.org"):
        # TODO: check if file is in settings.DLCS_SOURCE_BUCKET
        # if so, return that bucket's details
        # for testing this for now I will make it fetch from wl.org regardless,
        # as if for some reason it isn't in the storage bucket
        origin_info["web_url"] = origin
        # origin_info["bucket_name"] = foo 
        # origin_info["bucket_key"] = bar ... must check it's actually there
        return origin_info

    parts = origin.split("/")
    if parts[3] == settings.CURRENT_PRESERVATION_BUCKET:
        origin_info["bucket_name"] = settings.CURRENT_PRESERVATION_BUCKET
        origin_info["bucket_key"] = parts[4]

    return origin_info


def get_physical_file_maps(root):
    logging.info("building a map of the physical files to simplify lookups")
    physical_struct_map = root.find(
        "./mets:structMap[@TYPE='PHYSICAL']", namespaces)
    if physical_struct_map is None:
        logging.info("No physical struct map (may be anchor file)")
        return None

    tech_file_infos = {}
    amds = root.find("./mets:amdSec[@ID='AMD']", namespaces)
    logging.info("{0} tech mds to process".format(len(amds)))
    for tech_md in amds:
        adm_id = tech_md.get("ID")
        dg_file = tech_md.find(".//digiflow:File", namespaces)
        if dg_file is None:
            logging.info("No digiflow:File element for " + adm_id)

        else:
            adm_id = tech_md.get("ID")
            logging.info("adding " + adm_id + " to map")
            tech_file_infos[tech_md.get("ID")] = {
                "preservicaGuid": dg_file.find("digiflow:preservicaGuid", namespaces).text,
                "FileName": dg_file.find("digiflow:FileName", namespaces).text,
                "Checksum": dg_file.find("digiflow:Checksum", namespaces).text,
                "ChecksumAlgorithmRef": dg_file.find("digiflow:ChecksumAlgorithmRef", namespaces).text
            }  # add more props if we need them

    assets = {}
    alto = {}

    sequences = physical_struct_map.findall(
        "./mets:div[@TYPE='physSequence']", namespaces)
    for seq in sequences:
        phys_files = seq.findall("./mets:div", namespaces)
        for phys_file in phys_files:
            admin_md = phys_file.get("ADMID")
            tech_md = tech_file_infos[admin_md]
            summary = {
                "ADMID": admin_md,
                "ID": phys_file.get("ID"),
                "ORDER": phys_file.get("ORDER"),
                "ORDERLABEL": phys_file.get("ORDERLABEL"),
                "TYPE": phys_file.get("TYPE"),
                "tech_md": tech_md
            }
            for fptr in phys_file.findall("./mets:fptr", namespaces):
                file_id = fptr.get("FILEID")
                content_ids = fptr.get("CONTENTIDS")

                if file_id.endswith("_BAG") and content_ids is not None:
                    # sanity check
                    assert tech_md["preservicaGuid"] == content_ids, "Preservica GUID mismatch"
                    summary["preservica_guid"] = content_ids
                    summary["asset_file_id"] = file_id
                    assets[file_id] = summary

                elif file_id.endswith("_ALTO"):
                    summary["alto_file_id"] = file_id
                    alto[file_id] = summary

    return assets, alto


# helpers

def get_expanded_form(namespace, attr):
    return "{{{0}}}{1}".format(namespaces[namespace], attr)


def copy_simple_value_child(source_parent, source_name, target_parent, target_name):
    source = source_parent.find(source_name, namespaces)
    target = make_child_with_whitespace(target_parent, target_name)
    target.text = source.text


def make_child_with_whitespace(parent, name):
    # This isn't quite right but tidier than nothing
    child = ET.SubElement(parent, name)
    child.text = parent.text + '  '
    child.tail = parent.text
    return child


def remove_first_child(element):
    element.remove(element[0])


def normalise_b_number(b_str):
    if b_str[0] == "b":
        b_str = b_str[1:]

    # TODO - analyse digits, checksum etc
    # temporary -

    if len(b_str) != 8:
        raise ValueError('Not a valid b number')

    if b_str == "19974760":
        raise ValueError('Will not process Chemist and Druggist right now')

    return "b" + b_str


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


def clean_working_dir():
    for f in os.listdir(settings.WORKING_DIRECTORY):
        path = os.path.join(settings.WORKING_DIRECTORY, f)
        if os.path.isfile(path):
            os.remove(path)
        elif os.path.isdir(path):
            shutil.rmtree(path)


if __name__ == "__main__":
    main()
