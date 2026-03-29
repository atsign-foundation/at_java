# Changelog


## v0.0.1 (2026-03-29)

### Features
- release workflow to publish tagged versions
- add support for populating and using sharedKeyEnc (and associated fields)
- support for binary key values (#374)
- updated build to jdk 11 (#355)
- migrate to multimodule, consolidate dependency management and plugin configuration into parent pom. (#350)
- adds spotless and checkstyle plugins to maven build lifecycle. reformats and adjusts code to pass checkstyle, spotless and codeql rulesfeat: removed lint from markdown files
- support for apkam authentication model plus the onboarding and enrollment workflow. added support for ivNonce field in key metadata, enhanced implementation to use random IVs when encrypting/decrypting data, writing/reading iv to/from meta data. added version field to atKeys JSON

### Bug Fixes
- replace boilerplate code and consolidate common classes
- corrected javadoc. javadoc plugin is now configured to fail on error. checkstyle has been expanded to enforce class comments (#356)
- removed direct output to stderr and stdout in core library code, CLI and examples still use System.out/err, replaced with slf4j using lombok annotations
- enhance KeyStringUtil so that it parses namespaces (#326)


