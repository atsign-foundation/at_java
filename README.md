<!-- pyml disable-num-lines 4 md013,md033-->
<h1><a href="https://atsign.com#gh-light-mode-only">
   <img width=250px src="https://atsign.com/wp-content/uploads/2022/05/atsign-logo-horizontal-color2022.svg#gh-light-mode-only" alt="The Atsign Foundation"></a>
<a href="https://atsign.com#gh-dark-mode-only">
   <img width=250px src="https://atsign.com/wp-content/uploads/2023/08/atsign-logo-horizontal-reverse2022-Color.svg#gh-dark-mode-only" alt="The Atsign Foundation"></a></h1>

[![gitHub license](https://img.shields.io/badge/license-BSD3-blue.svg)](./LICENSE)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/atsign-foundation/at_java/badge)](https://securityscorecards.dev/viewer/?uri=github.com/atsign-foundation/at_java&sort_by=check-score&sort_direction=desc)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/8116/badge)](https://www.bestpractices.dev/projects/8116)

## The AtPlatform for Java developers

This repository contains libraries, tools, samples and examples for developers that
wish to work with the AtPlatform from Java code.

## Modules

There are 4 modules.

* **at_client** is the Java SDK for interacting with the AtPlatform
  [README](at_client/README.md).
* **at_shell** is a self-contained "fatjar" that provides an interactive command
  line interface for performing various operations and tests
  [README](at_shell/README.md).
* **at_utils** contains useful code which is not part of the SDK
  [README](at_utils/README.md).
* **examples** contains sample code which illustrates how to use the SDK
  [README](examples/README.md).

## Build Process

### Prerequisites

* Java (JDK 11 or above)
* Maven (3.6.3 or above)

Check out, build and test with the following commands:

```text
git clone https://github.com/atsign-foundation/at_java.git
mvn install
```

**Note:** The integration tests rely on the virtual env. The tests
will attempt to start a docker container with this image but that relies
on dockerd (or desktop docker). If you want to skip these tests then add
**-DskipITs**. See subsequent section for more information.

## Unit Tests

These have no external dependencies and are run as part of the maven **test**
lifecycle. The surefire plugin will pick up classes that end in the following:

```text
**/*Test.java
**/*Tests.java
**/*TestCase.java
```

Use **-DskipTests** to avoid running unit tests.

### Integration Tests

These depend on running the atsign virtual environment and are run as part
of the maven **verify** lifecycle. The failsafe plugin will pick up classes that
end in the following:

```text
**/*IT.java
```

Use **-DskipITs** to avoid running integration tests.

The integration tests check to see if a virtual env is running. If not they will
attempt to start one. This requires dockerd or desktop docker to be running.

For CI, "standing up" and then "tearing down" the docker container is the intended
behavior. However, for a developer this will be slow so it's preferable to "standup"
the virtual env independently, like this:

```shell
cd at_client/src/test/resources/org/atsign/virtualenv
docker compose -f up
```

The start-up can take a few minutes and involves running scripts that install the
test configuration. The environment is ready to test with when you see the
following log output:

```text
...
virtualenv-1  | SHOUT|...| install_PKAM_Keys |cramAndPkamAuth successful for @chris
virtualenv-1  | SHOUT|...| install_PKAM_Keys |cramAndPkamAuth successful for @policy1
virtualenv-1  | SHOUT|...| install_PKAM_Keys |cramAndPkamAuth successful for @emoji
```

The tests require test keys (CRAM keys and atKeys) which the virtual env was
built with. The pom contains a plugin which downloads the
[at_demo_data](https://pub.dev/packages/at_demo_data) package and unpacks it
under target. When the tests run this is where they expect to find keys. The
release version is specified as property in the POM and needs to be periodically
updated to the latest release.

**Note:** The integration tests are designed to reset the virtual env as best as
possible. This relies on teardown steps. If you are debugging tests and terminate
the execution then this can leave the virtual env in an unreset state. Running
the test again might fail because of this BUT that teardown should successfully
reset the env and allow the next run to succeed. In extreme circumstances if may
be necessary to reset the docker container, like this:

```shell
cd at_client/src/test/resources/org/atsign/virtualenv
docker compose -f down
docker compose -f up
```

## Virtual Environment

This is a docker image that bundles the following:

* redis
* root server
* multiple at_servers in different states of configuration

The docker image is published as part of the
[at_server](https://github.com/atsign-foundation/at_server) repository.

The keys (CRAM secrets, pre-cut AtKeys) are part the
[at_demos](https://github.com/atsign-foundation/at_demos) repository.

The pom.xml for modules which run have integration tests include a plugin
which downloads the at_demos package to target.

## Code Style And Formatting

The maven poms contain the following plugins to enforce a consistent coding
style and format.

* [checkstyle](https://checkstyle.org)
* [spotless](https://github.com/diffplug/spotless)

The rules which configure the respective plugins are here

* [checkstyle.xml](config/checkstyle.xml)
* [java-format.xml](config/java-format.xml)

Run the following maven command run the checks:

```shell
mvn validate
```

Run the following maven command to fix the spotless violations:

```shell
mvn spotless:apply
```

### Intellij

To configure Intellij to use the same settings
1. Add the **Adaptor for Eclopse Code Formatter** plugin and configure in
**Settings -> Adaptor for Eclipse Code Formatter** by setting
**Eclipse workspace/project folder or config file** as
config/java-format.xml
2. Add **CheckStyle-IDEA** plugin and configure in
**Settings -> Tools -> Checkstyle** by adding config/checkstyle.xml

## Contributions welcome

All of our software is open with intent. We welcome contributions - we want
pull requests, and we want to hear about issues. See also
[CONTRIBUTING.md](CONTRIBUTING.md)
