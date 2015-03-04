# Object Generator

## Introduction
Object Generator (OG) is an http load tool designed for load testing object
storage apis. Supported apis include:

- SOH
- S3
- Openstack
- WebDAV

## Build
To build og, maven 3 is required. Run the following maven command in the root
of this repository:
    mvn clean install

Alternatively, to skip unit tests:
    mvn clean install -DskipTests=true

To build with a custom display version (useful for continuous integration):
    mvn clean install -Ddisplay.version=<custom_version>

The resulting archive can be found in _og-assembly/target/_.

## Documentation
For more information, build the tool and view documentation.html in the
resulting archive.
