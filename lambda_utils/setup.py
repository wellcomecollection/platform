import os

from setuptools import find_packages, setup


def local_file(name):
    return os.path.relpath(os.path.join(os.path.dirname(__file__), name))


SOURCE = local_file('src')

setup(
    name='wellcome_lambda_utils',
    packages=find_packages(SOURCE),
    package_dir={'': SOURCE},
    version='1.0.6',
    install_requires=[
        'attrs>=17.2.0',
        'boto3',
        'daiquiri>=1.3.0',
        'python-dateutil',
        'requests>=2.18.4',
    ],
    python_requires='>=3.6',
    description='Common lib for lambdas',
    author='Wellcome digital platform',
    author_email='wellcomedigitalplatform@wellcome.ac.uk',
    url='https://github.com/wellcometrust/platform',
    keywords=['lambda', 'utils'],
    classifiers=[],
)
