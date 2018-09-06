from requests import get, auth
import settings


def get_image(image_slug, space=None):
    if space is None:
        space = settings.DLCS_SPACE

    url = "{0}spaces/{1}/images/{2}".format(get_customer_url(), space, image_slug)
    response = get(url, auth=get_authorisation())
    return response.json()


def get_authorisation():
    return auth.HTTPBasicAuth(settings.DLCS_API_KEY, settings.DLCS_API_SECRET)


def get_customer_url():
    return "{0}customers/{1}/".format(settings.DLCS_ENTRY, settings.DLCS_CUSTOMER_ID)
