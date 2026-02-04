# AtSign Java SDK
This module contains the AtSign Java SDK.

Add the following Maven configuration to your pom in order to use the latest version of this library.

```xml
  <repositories>
    <repository>
      <name>Central Portal Snapshots</name>
      <id>central-portal-snapshots</id>
      <url>https://central.sonatype.com/repository/maven-snapshots</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>org.atsign</groupId>
      <artifactId>at_client</artifactId>
      <version>0.0.1-SNAPSHOT</version>
    </dependency>
  </dependencies>
```

## Dependencies

The library currently depends on the following

* Bouncycastle
* Jackson
* Apache Commons (will be removed)
* Pico CLI

## Developer Instructions

The following command will compile, run unit tests and integration tests

```shell
mvn clean install
```

### Unit Tests

These have no external dependencies. 

The following command will compile and run these

```shell
mvn clean test
```

The surefire plugin should pick up classes that end in the following

```text
**/*Test.java
**/*Tests.java
**/*TestCase.java
```

### Integration Tests

These depend on running the at virtual environment. The integration tests check to see if a virtual env is running. 
If not they will attempt to start one. This requires dockerd or desktop docker to be running. For CI standing up
and then tearing down the docker container is the intended behavior. For a developer this can be expensive so it's
preferable to "standup" the virtual env independently.

```shell
cd src/test/resources/org/atsign/virtualenv
docker compose up
```

The start up can take a few minutes and involves running scripts that install test configuration. The environment
is ready to test with when you see the following log output

```text
...
virtualenv-1  | SHOUT|2026-02-04 07:16:40.933352| install_PKAM_Keys |cramAndPkamAuth successful for @chris
virtualenv-1  | SHOUT|2026-02-04 07:16:40.933750| install_PKAM_Keys |cramAndPkamAuth successful for @policy1
virtualenv-1  | SHOUT|2026-02-04 07:16:40.934626| install_PKAM_Keys |cramAndPkamAuth successful for @emoji
```

The following command will compile, run unit tests and then run the integration tests

```shell
mvn clean verify
```

The tests require test keys (CRAM keys and atKeys)  which the virtual env was built with. The pom contains a plugin
which downloads the at_demo_data package [https://pub.dev/packages/at_demo_data](https://pub.dev/packages/at_demo_data)
and unpacks it under target. The release version is specified as property in the POM and needs to be periodically
updated to the latest release.

The failsafe plugin should pick up classes that end in the following

```text
**/*IT.java
```

### Virtual Environment

This is a docker image that bundles the following.
* redis
* root server
* multiple pre-configured at servers

This docker image is built as part of this repo

[https://github.com/atsign-foundation/at_server](https://github.com/atsign-foundation/at_server)

The keys (CRAM secrets, pre-cut AtKeys) are part of this repo

[https://github.com/atsign-foundation/at_demos](https://github.com/atsign-foundation/at_demos)

