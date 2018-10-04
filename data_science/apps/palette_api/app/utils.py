from skimage.color import rgb2lab


def colour_distance(colour_1, colour_2):
    return sum([(a - b) ** 2 for a, b in zip(colour_1, colour_2)]) ** 0.5


def hex_to_rgb(hex):
    return [int(hex[i : i + 2], 16) for i in range(0, 6, 2)]


def rgb_to_lab(rgb_palette):
    return rgb2lab(rgb_palette.reshape(-1, 1, 3) / 255).squeeze()


def ids_to_urls(image_ids):
    url = "https://iiif.wellcomecollection.org/image/{}/full/300,/0/default.jpg"
    return [url.format(image_id) for image_id in image_ids]
