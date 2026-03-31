# AtTalk Shell

Illustrative command line utility which uses the Java at_client library to provide a simple chat tool

## Usage

To see usage:

```shell
java -jar target/at_talk-0.0.1-SNAPSHOT.jar --help
```

To run shell against virtual env as @gary talking to @colin, run this...

```shell
java -jar target/at_talk-0.0.1-SNAPSHOT.jar -a gary -t colin -d vip.ve.atsign.zone
```

And then run the shell against the virtual env as @colin talking to @gary, run this...

```shell
java -jar target/at_talk-0.0.1-SNAPSHOT.jar -a colin -t gary -d vip.ve.atsign.zone
```

**Note:** This will default to looking in $HOME/.atsign for AtKeys.

There are 3 ways to resolve this...

1. Copy the demo data keys to to $HOME/.atsign
2. Override the default location and key file suffix with java properties.
3. Override the default location and key file suffix with environment properties.

### Copying the demo data key files

Copy all the files target/at_demo_data/lib/assets/atkeys to $HOME/.atsign
AND rename the key files so that they have the suffux _key.atKeys.

### Override with Java properties

Add the following -D options to your command lines.

```shell
java -DATSIGN_KEYS_DIR=target/at_demo_data/lib/assets/atkeys \
  -DATSIGN_KEYS_SUFFIX=.atKeys \
  -jar target/at_talk-0.0.1-SNAPSHOT.jar -a gary -t colin -d vip.ve.atsign.zone
```

### Override with environment variables

Set/export the following environment variables prior to running the shell.

| Environment Variable | Value                                 |
|----------------------|---------------------------------------|
| ATSIGN_KEYS_DIR      | target/at_demo_data/lib/assets/atkeys |
| ATSIGN_KEYS_SUFFIX   | .atKey                                | 

**Note:** JDK 9+ has stricter JAR verification for cryptographic providers.
This means that BouncyCastle cannot be included in the shaded jar (this would
remove the signature). The maven build downloads the bouncycastle jar to
target/lib and include this in the jar manifest classpath. If you copy the
FAT jar elsewhere you need to ensure that lib dir, containing the bouncycastle
jar is also copied too.
