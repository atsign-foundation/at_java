# Changelog

## v1.0.0 (2026-04-11)

### Features
- at_talk cli plus monitor modifications
- remove CompletableFuture pattern from AtClient interface

### Bug Fixes
- fixed bug in test that was failing sometimes
- incremented version for jackson and netty to vulnerabilities free versions

## v0.0.1 (2026-03)

### Features
- at_talk cli plus monitor modifications
- remove CompletableFuture pattern from AtClient interface
- add support for populating and using sharedKeyEnc (and associated fields)
- support for binary key values (#374)
- updated build to jdk 11 (#355)
- migrate to multimodule, consolidate dependency management and plugin configuration into parent pom. (#350)

### Bug Fixes
- incremented version for jackson and netty to vulnerabilities free versions
push- replace boilerplate code and consolidate common classes
- corrected javadoc. javadoc plugin is now configured to fail on error. checkstyle has been expanded to enforce class comments (#356)
- removed direct output to stderr and stdout in core library code, CLI and examples still use System.out/err, replaced with slf4j using lombok annotations

