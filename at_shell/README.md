# Java Shell

Interactive command line utility which uses the Java at_client library

## Usage

To see usage:

```shell
java -jar target/at_shell-0.0.1-SNAPSHOT.jar
```

To run shell against virtual env as @gary:

```shell
java -jar target/at_shell-0.0.1-SNAPSHOT.jar vip.ve.atsign.zone:64 @gary
```

**Note:** This will default to looking in $HOME/.atsign for AtKeys.

To point to the at_demo_data package keys:

```shell
java -DATSIGN_KEYS_DIR=target/at_demo_data/lib/assets/atkeys \
  -DATSIGN_KEYS_SUFFIX=.atKeys \
  -jar target/at_shell-0.0.1-SNAPSHOT.jar vip.ve.atsign.zone:64 @gary
```

**Note:** JDK 9+ has stricter JAR verification for cryptographic providers.
This means that BouncyCastle cannot be included in the shaded jar (this would
remove the signature). The maven build downloads the bouncycastle jar to
target/lib and include this in the jar manifest classpath. If you copy the
FAT jar elsewhere you need to ensure that lib dir, containing the bouncycastle
jar is also copied too.
