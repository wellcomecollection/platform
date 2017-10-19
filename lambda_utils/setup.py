import os

from setuptools import find_packages, setup


def local_file(name):
    return os.path.relpath(os.path.join(os.path.dirname(__file__), name))


SOURCE = local_file('src')

setup(
    name='wellcome_lambda_utils',
    packages=find_packages(SOURCE),
    package_dir={'': SOURCE},
    version='1.0.3',
    install_requires=['boto', 'dateutil'],
    python_requires='>=3.6',
    description='Common lib for lambdas',
    author='Wellcome digital platform',
    author_email='wellcomedigitalplatform@wellcome.ac.uk',
    url='https://github.com/wellcometrust/platform',
    keywords=['lambda', 'utils'],
    classifiers=[],
)
