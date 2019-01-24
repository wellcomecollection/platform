import logging
from xml_help import namespaces, expand
import mappings

# Borrowed from Dds.Dashboard, LogicalStructDiv impl

collection_types = ["MultipleManifestation", "Periodical", "PeriodicalVolume"]

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
    "Audio",
    "Map",
]


def is_collection(struct_type):
    return struct_type in collection_types


def is_manifestation(struct_type):
    return struct_type in manifestation_types


def get_logical_struct_div(root):
    logical_struct_map = root.find("./mets:structMap[@TYPE='LOGICAL']", namespaces)
    assert len(logical_struct_map) == 1, "Expect one element in logical structMap"
    return logical_struct_map[0]


def get_file_pointer_link(struct_div):
    """
    Expects to find a pointer to another METS file, rather than METS content.
    This is for METS files linking to other METS files, both parent=>child and child=>parent.
    Sometimes, Goobi doesn't include the xml extension for the linked file.
    :rtype: str
    :param struct_div: The struct div element to extract the link from
    :return: the link path
    """
    tag = struct_div[0].tag
    expected = expand("mets", "mptr")
    assert tag == expected, "expected {0}, found {1}".format(expected, tag)
    assert struct_div[0].get("LOCTYPE") == "URL", "Pointer is not URL"
    xlink_href = expand("xlink", "href")
    link = struct_div[0].get(xlink_href)
    if not link.endswith(".xml"):
        logging.debug("Adding .xml to mets file pointer")
        link = link + ".xml"
        struct_div[0].set(xlink_href, link)

    logging.debug("obtained link from pointer: " + link)
    return link


def remove_deliverable_unit(root):
    """
The Deliverable Unit concept is no longer required in the METS file.
When removing it, we also want to renumber the remaining AMD blocks,
giving them IDs that start at 0001.

We need to remove the AMD for the DeliverableUnit, then renumber
the AMD to start at _0001

<mets:amdSec ID="AMD">
  <mets:techMD ID="AMD_0001">        <== remove this...
    <mets:mdWrap MDTYPE="OTHER" MIMETYPE="text/xml">
      <mets:xmlData>
        <tessella:DeliverableUnit>   <== having found this

    :param root: The METS file
    """
    logging.debug("Looking at DeliverableUnit")
    x_path = ".//mets:xmlData[tessella:DeliverableUnit]"
    du_xmldata = root.findall(x_path, namespaces)
    if len(du_xmldata) != 1:
        raise KeyError("Expecting one DeliverableUnit XML block")

    amd_sec = root.find("./mets:amdSec[@ID='AMD']", namespaces)
    # assume the first tech_md is the deliverable unit - if it isn't,
    # this is an unusual METS so we should bail out at this point anyway
    du = amd_sec[0].find(
        "./mets:mdWrap/mets:xmlData/[tessella:DeliverableUnit]", namespaces
    )
    assert du is not None, "The first techMD is not the deliverable unit"
    amd_sec.remove(amd_sec[0])

    counter = 1
    ignore = ["RIGHTS", "DIGIPROV"]
    for tech_md in amd_sec:
        old_id = tech_md.get("ID")
        if old_id in ignore:
            continue

        refs = root.findall(".//mets:div[@ADMID='{0}']".format(old_id), namespaces)
        if len(refs) == 0 and is_ignorable_file(tech_md):
            amd_sec.remove(tech_md)
            continue

        new_id = "AMD_" + str(counter).zfill(4)
        tech_md.set("ID", new_id)
        assert len(refs) == 1, "Expected 1 AMD ref for {0}, got {1}".format(
            old_id, len(refs)
        )
        refs[0].set("ADMID", new_id)
        counter = counter + 1


def is_ignorable_file(tech_md):
    file_name_els = tech_md.findall(
        "./mets:mdWrap/mets:xmlData/tessella:File/tessella:FileName", namespaces
    )
    if len(file_name_els) == 1:
        logging.debug("ignoring techMd file " + file_name_els[0].text)
        return file_name_els[0].text in mappings.IGNORED_TECHMD_FILENAMES
    return False


def remodel_file_section(root):
    logging.debug("transforming file section")
    sdb_file_group = root.find("./mets:fileSec/mets:fileGrp[@USE='SDB']", namespaces)
    sdb_file_group.set("USE", "OBJECTS")
    for sdb_file in sdb_file_group:
        sdb_file_id = sdb_file.get("ID")
        bag_file_id = sdb_file_id.replace("_SDB", "_OBJECTS")
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


def get_physical_file_maps(root):
    logging.debug("building a map of the physical files to simplify lookups")
    physical_struct_map = root.find("./mets:structMap[@TYPE='PHYSICAL']", namespaces)
    if physical_struct_map is None:
        logging.debug("No physical struct map (may be anchor file)")
        return None

    tech_file_infos = {}
    amds = root.find("./mets:amdSec[@ID='AMD']", namespaces)
    logging.debug("{0} tech mds to process".format(len(amds)))
    for tech_md in amds:
        adm_id = tech_md.get("ID")
        premis_object = tech_md.find(".//premis:object", namespaces)
        if premis_object is None:
            logging.debug("No premis:object element for " + adm_id)

        else:
            adm_id = tech_md.get("ID")
            logging.debug("adding " + adm_id + " to map")
            uuid_el = premis_object.find(
                "./premis:objectIdentifier[premis:objectIdentifierType='uuid']",
                namespaces,
            )
            uuid_value = uuid_el.find("./premis:objectIdentifierValue", namespaces).text
            tech_file_infos[tech_md.get("ID")] = {
                "uuid": uuid_value
            }  # add more props if we need them

    assets = {}
    alto = {}

    sequences = physical_struct_map.findall(
        "./mets:div[@TYPE='physSequence']", namespaces
    )
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
                "tech_md": tech_md,
            }
            for fptr in phys_file.findall("./mets:fptr", namespaces):
                file_id = fptr.get("FILEID")
                content_ids = fptr.get("CONTENTIDS")

                if file_id.endswith("_OBJECTS") and content_ids is not None:
                    # sanity check
                    assert tech_md["uuid"] == content_ids, "Preservica GUID mismatch"
                    summary["asset_file_id"] = file_id
                    assets[file_id] = summary
                    fptr.attrib.pop("CONTENTIDS")

                elif file_id.endswith("_ALTO"):
                    summary["alto_file_id"] = file_id
                    alto[file_id] = summary

    return assets, alto


def get_title(mets_root):
    title_el = mets_root.findall(".//mods:title", namespaces)
    return title_el[0].text
