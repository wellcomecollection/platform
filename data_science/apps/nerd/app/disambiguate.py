import wikipedia
from extract import extract_entities


def _disambiguate(found_entity):
    """
    given an entity name, return a corresponding wikipedia url
    """
    search_results = wikipedia.search(found_entity)
    best_candidate = search_results[0]
    url = wikipedia.page(best_candidate).url
    return ' <a href=\"{0}\">{1}</a> '.format(url, found_entity)


def disambiguate(set_of_entities, with_failures=True):
    """
    do the same thing for a list
    """
    disambiguated_dict = {}
    for ent in set_of_entities:
        try:
            disambiguated_dict[ent] = _disambiguate(ent)
        except:
            if with_failures:
                disambiguated_dict[ent] = (
                    'could not disambiguate "{}"'.format(ent)
                )
            else:
                pass
    return disambiguated_dict


def annotate_with_links(text):
    """
    takes a block of text and annotates it with links to the wikipedia pages 
    of any found entities
    """
    ents = extract_entities(text)
    disambiguated_dict = disambiguate(ents, with_failures=False)
    sorted_ents = sorted(disambiguated_dict.keys(), key=len)[::-1]

    hash_dict = {}
    for ent in sorted_ents:
        hashed_ent = str(hash(ent))
        hash_dict[hashed_ent] = disambiguated_dict[ent]
        text = text.replace(ent, hashed_ent)

    for hashed_ent, link in hash_dict.items():
        text = text.replace(hashed_ent, link)

    return text
