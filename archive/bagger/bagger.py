import sys
import os
import shutil
import logging
import xml.etree.ElementTree as ET
import boto3
import bagit
import settings

logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.DEBUG)

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
    b_number = normalise_b_number(sys.argv[1])
    mets_path = get_mets_path(b_number)
    logging.info("process METS or anchor file at %s", mets_path)
    tree = load_xml(mets_path)
    root = tree.getroot()

    # TODO - is this an anchor file or a regular METS?

    # mets:dmdSec * 2
    # Leave the METS header and dmdSecs the same
    # ----

    logging.info("We will transform (minimally) data that involves Preservica, and the tessella namespace")
    logging.info("What follows is not a recommendation at this stage, ")
    logging.info("just a recognition that these parts of METS will be different.")

    remodel_deliverable_unit(root)
    remodel_file_technical_metadata(root)
    remodel_file_section(root)
    
    make_bag(tree, b_number)


def load_xml(path):
    if settings.METS_FILESYSTEM_ROOT:
        return ET.parse(settings.METS_FILESYSTEM_ROOT + path)
    
    s3 = get_boto_session().resource("s3")
    obj = s3.Object(settings.METS_BUCKET_NAME, path)
    xml_string = obj.get()["Body"].read().decode("utf-8")
    root = ET.fromstring(xml_string)
    return ET.ElementTree(root)
    

def get_boto_session():
    global boto_session
    if boto_session is None:
        boto_session = boto3.Session(
            aws_access_key_id = settings.AWS_PUBLIC_KEY,
            aws_secret_access_key = settings.AWS_SECRET_KEY,
            region_name = settings.AWS_REGION
        )
    return boto_session

    
def make_bag(tree, b_number):
    # TODO - this is not making the bag properly yet, it's just setting up the end to end pipe,
    # and only deals with one thing
    b_form = "b{0}".format(b_number)
    zip_file_name = "{0}.zip".format(b_form)
    bag_dir = os.path.join(settings.WORKING_DIRECTORY, b_form)
    zip_file_path = os.path.join(settings.WORKING_DIRECTORY, zip_file_name)
    shutil.rmtree(bag_dir, ignore_errors=True)
    try:
        os.remove(zip_file_path)
    except OSError:
        pass

    if os.path.isdir(bag_dir):
        raise FileExistsError("Unable to start with clean bag_dir")
    if os.path.isfile(zip_file_path):
        raise FileExistsError("Unable to remove existing zipped bag")

    # bagit makes the data directory itself
    os.makedirs(os.path.join(bag_dir, "objects"))
    tree.write(os.path.join(bag_dir, "{0}.xml".format(b_form)), encoding="utf-8")
    bag = bagit.make_bag(bag_dir, {"Contact-Name": "Tom"})

    # now zip this bag in a way that will be efficient for the archiver
    # for now, this:
    shutil.make_archive(zip_file_path[0:-4], 'zip', bag_dir)

    s3 = get_boto_session().client("s3")
    s3.upload_file(zip_file_path, settings.DROP_BUCKET_NAME, zip_file_name)
    # and put it in the drop bucket

    # xml_string = ET.tostring(root, encoding="utf-8")
    # 
    # response = s3.put_object( 
    #     Bucket = settings.DROP_BUCKET_NAME,
    #     Body = xml_string,
    #     Key = "b{0}/b{0}.xml".format(b_number)
    # )



def remodel_deliverable_unit(root):
    logging.info("Looking at DeliverableUnit")
    du_xmldata = root.findall(".//mets:xmlData[tessella:DeliverableUnit]", namespaces)
    if len(du_xmldata) != 1:
        raise KeyError("Expecting one DeliverableUnit XML block")
    
    # Where does the PurposeName come from?
    # the ID comes from Preservica (?) and can be dropped?
    new_deliverablue_unit = make_child_with_whitespace(du_xmldata[0], 'digiflow:DeliverableUnit') 
    purpose_name_element = make_child_with_whitespace(new_deliverablue_unit, "digiflow:PurposeName")
    purpose_name_element.text = du_xmldata[0][0].find("tessella:PurposeName", namespaces).text
    remove_first_child(du_xmldata[0])


def remodel_file_technical_metadata(root):    
    logging.info("transforming Tessella techMD")
    tessella_file_xmldata = root.findall(".//mets:xmlData[tessella:File]", namespaces)
    for xmldata in tessella_file_xmldata:
        tessella_file = xmldata[0]
        new_file = make_child_with_whitespace(xmldata, "digiflow:File")
        carried_over_properties = [
            "FileName", "Folder", "FormatName", "FileSize", "Checksum", "ChecksumAlgorithmRef"
        ]
        for prop in carried_over_properties:
            copy_simple_value_child(tessella_file, "tessella:" + prop, 
                                    new_file, "digiflow:" + prop)
        # we'll remove this one later
        copy_simple_value_child(tessella_file, "tessella:ID", new_file, "digiflow:tempID")

        remove_first_child(xmldata)
        file_properties = tessella_file.findall("tessella:FileProperty", namespaces)
        to_copy = ["Image Height", "Image Width"] # more to come
        for file_property in file_properties:
            name = file_property.find("tessella:FilePropertyName", namespaces).text.strip()
            if name in to_copy:
                value = file_property.find("tessella:Value", namespaces).text.strip()
                new_prop = make_child_with_whitespace(new_file, "digiflow:FileProperty")
                make_child_with_whitespace(new_prop, "digiflow:FilePropertyName").text = name
                make_child_with_whitespace(new_prop, "digiflow:Value").text = value
                

def remodel_file_section(root):
    logging.info("transforming file section")
    sdb_file_group = root.find("./mets:fileSec/mets:fileGrp[@USE='SDB']", namespaces)
    sdb_file_group.set('USE', 'BAGGER')
    for sdb_file in sdb_file_group:
        sdb_file_id = sdb_file.get("ID")
        bag_file_id = sdb_file_id.replace("_SDB", "_BAG")
        if sdb_file_id == bag_file_id:
            # OK, but what is the strategy here...?
            # This will be OK for experiments
            raise ValueError("SDB ID didn't contain '_SDB'")
        sdb_file.set("ID", bag_file_id)

        locator = sdb_file.find("mets:FLocat", namespaces)
        current_location = locator.get(get_attr_name("xlink", "href"))
        desired_relative_location = "objects/{0}".format(current_location)
        locator.set(get_attr_name("xlink", "href"), desired_relative_location)
        # TODO: at this point:
        # fetch this file, from Preservica local or cloud storage
        # validate checksum
        # store in correct relative location

        # find pointers to this file
        xpath = ".//mets:fptr[@FILEID='{0}']".format(sdb_file_id)
        file_pointers = root.findall(xpath, namespaces)
        for file_pointer in file_pointers:
            file_pointer.set("FILEID", bag_file_id) 




## helpers

def get_attr_name(namespace, attr):
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
    # TODO - handle 7 digit and deduce checksum
    if b_str[0] == "b":
        b_str = b_str[1:]
    
    if len(b_str) == 8:
        return b_str
    
    raise ValueError('Not a valid b number')


def get_mets_path(b_number):
    separator = "/"
    if settings.METS_FILESYSTEM_ROOT:
        separator = "\\"

    pathparts = separator.join(b_number[8:3:-1])
    return "{0}{1}{2}b{3}.xml".format(settings.METS_ROOT_PREFIX, pathparts, separator, b_number)


    
if __name__ == "__main__":
    main()
