# Object Generator

## Preface

## Introduction
__Object Generator (OG)__ is an http load tool designed for load testing object
storage apis. Supported apis include:

- SOH
- S3
- OpenStack
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
For more information, browse the [userguide](./og-assembly/src/main/asciidoc/userguide.adoc) or build the tool and
view _docs/userguide.html_ in the resulting archive.
