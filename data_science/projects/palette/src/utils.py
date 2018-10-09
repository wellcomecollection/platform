from PIL import Image
import numpy as np
from skimage.color import rgb2lab, lab2rgb
from sklearn.cluster import KMeans
from scipy.optimize import linear_sum_assignment


def get_palette(image, palette_size=5, image_size=75):
    """
    Return n dominant colours for a given image

    Parameters
    ----------
    image : PIL.Image
        The image for which we want to create a palette of dominant colours
    palette_size :
        The number of dominant colours to extract
    image_size :
        Images are resized and squared by default to reduce processing time.
        This value sets the side-length of the square. Higher values will
        indrease fidelity.

    Returns
    -------
    palette : np.array
        palette coordinates in LAB space
    """
    image = image.resize((image_size, image_size))
    lab_image = rgb2lab(np.array(image)).reshape(-1, 3)
    clusters = KMeans(n_clusters=palette_size).fit(lab_image)
    return clusters.cluster_centers_


def display_palette(palette_colours, image_size=100, big=False):
    """
    Return n dominant colours for a given image

    Parameters
    ----------
    palette_colours : np.array
        palette coordinates in LAB space
    image_size :
        The size of each palette colour swatch to be returned

    Returns
    -------
    palette : PIL.Image
        image of square colour swatches
    """
    palette_size = len(palette_colours)

    scale = 1

    if big:
        scale = 5

    stretched_colours = [
        (
            lab2rgb(
                np.array(colour.tolist() * image_size * image_size * scale).reshape(
                    image_size * scale, image_size, 3
                )
            )
            * 255
        ).astype(np.uint8)
        for colour in palette_colours
    ]

    palette_array = np.hstack(stretched_colours).reshape(
        (image_size * scale, image_size * palette_size, 3)
    )

    return Image.fromarray(palette_array)


def colour_distance(colour_1, colour_2):
    return sum([(a - b) ** 2 for a, b in zip(colour_1, colour_2)]) ** 0.5


def palette_distance(palette_1, palette_2):
    distances = [[colour_distance(c_1, c_2) for c_2 in palette_2] for c_1 in palette_1]

    _, rearrangement = linear_sum_assignment(distances)
    palette_1 = [palette_1[i] for i in rearrangement]

    palette_distance = sum(
        [colour_distance(c_1, c_2) for c_1, c_2 in zip(palette_1, palette_2)]
    )

    return palette_distance


def moving_average(arr, n):
    """
    Returns a moving average over a given array

    Parameters
    ----------
    arr : numpy.array
        input array
    n : int
        window size

    Returns
    -------
    arr : numpy.array
        input array with moving average applied
    """
    cumsum = np.cumsum(arr)
    return (cumsum[n:] - cumsum[:-n])[n - 1 :] / n


def smooth_histogram(hist, n=10):
    """
    applies a moving average to a image histogram, retaining separation between
    the 3 channels

    Parameters
    ----------
    hist : numpy.array
        flat input histogram of size=(768,)
    n : int
        window size

    Returns
    -------
    arr : numpy.array
        input array with moving average applied
    """
    r, g, b = hist.reshape(-1, 3).T
    return np.concatenate(
        [moving_average(r, n), moving_average(g, n), moving_average(b, n)]
    )
