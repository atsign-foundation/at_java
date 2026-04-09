# Java Client SDK

This module contains the AtSign Java SDK.

## Using the SDK

If you are using maven, add the following to your pom.xml

```xml

<dependencies>
  <dependency>
    <groupId>org.atsign</groupId>
    <artifactId>at_client</artifactId>
    <version>0.0.3</version>
  </dependency>
</dependencies>
```

If you are using gradle, add the following to your build.gradle

```text
dependencies {
    implementation 'org.atsign:at_client:1.0.0'
}
```

### Snapshot Version

The latest snapshot version can be added as a maven dependency like this...

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
  <version>0.0.4-SNAPSHOT</version>
</dependency>
</dependencies>
```

## Dependencies

The SDK currently depends on the following

* Bouncycastle (encryption, decryption, and cryptography utilities)
* Netty (networking)
* Jackson (support for JSON and YAML encoding and decoding)
* Pico CLI (lightweight command line interface framework)
* Slf4j api (Simple logging facade that can be bound a variety of logging
  frameworks)

## Logging

The SDK uses the slf4j-api. It is up to you to decide which slf4j binding to use.
See [slf4j providers](https://slf4j.org/manual.html#swapping).

If you are simply executing the example code below then you can just add slf4j-simple
as a dependency in your pom.xml.

```xml
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>0.0.3</version>
  </dependency>
```

## Getting Started

The classes **org.atsign.client.impl.AtCommandExecutors** and 
**org.atsign.client.impl.AtClients** 
provide default implementations of the AtSign SDK interfaces (
**org.atsign.client.api.AtCommandExecutor** and 
**org.atsign.client.api.AtClient** respectively).

An **org.atsign.client.api.AtCommandExecutor** represents something
that can send At Protocol commands to an At Server (including the root
/ directory sever).

The package [commands](src/org/atsign/client/impl/commands) contains utility code
which implements the 
[At Protocol Specification ](https://github.com/atsign-foundation/at_protocol/blob/trunk/specification/at_protocol_specification.md)
, for instance...

* Authentication
* Notifications
* Verifying At Server responses
* Signing public key values
* End to end encryption / decryption for self keys and shared keys

An **org.atsign.client.api.AtClient** provides a "map like" interface to
an At Server. Enabling you to put, get and delete the different types of
keys. By combining a default **org.atsign.client.api.AtCommandExecutor** and
the utility code in the **commands** package it provides the core capabilities
of the Java SDK.

The following code illustrates putting some key values.

```java
package org.example;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.impl.AtClients;

public class Main {

   public static void main(String[] args) throws Exception {

      AtSign atSign = AtSign.createAtSign("atsign1");

      try (AtClient client = AtClients.builder().atSign(atSign).build()) {

         Keys.PublicKey key = Keys.publicKeyBuilder()
                 .sharedBy(atSign)
                 .name("greeting")
                 .build();

         client.put(key, "hello");

         AtSign anotherAtSign = AtSign.createAtSign("atsign2");

         Keys.SharedKey anotherKey = Keys.sharedKeyBuilder()
                 .sharedBy(atSign)
                 .sharedWith(anotherAtSign)
                 .name("greeting")
                 .build();

         client.put(anotherKey, "hola");

      }

   }
}
```
And this code illustrates creating getting the key values that were put in the 
previous example code.

```java
package org.example;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;
import org.atsign.client.impl.AtClients;

public class Main {

    public static void main(String[] args) throws Exception {

        AtSign atSign = AtSign.createAtSign("atsign2");

        try (AtClient client = AtClients.builder().atSign(atSign).build()) {

            AtSign anotherAtSign = AtSign.createAtSign("atsign1");

            Keys.PublicKey key = Keys.publicKeyBuilder()
                    .sharedBy(anotherAtSign)
                    .name("greeting")
                    .build();

            System.out.println(client.get(key));

            Keys.SharedKey anotherKey = Keys.sharedKeyBuilder()
                    .sharedBy(anotherAtSign)
                    .sharedWith(atSign)
                    .name("greeting")
                    .build();

            System.out.println(client.get(anotherKey));

        }

    }
}
```
The **AtClients.builder()** will create an **AtCommandExecutor** which will resolve the
endpoint for the atsign's at server from **root.atsign.org**. The **AtCommandExector**
will automatically retry connections and reconnect if it becomes disconnected. The
commands that are sent to the asign atserver will automatically timeout. The builder
will automatically load **AtKeys** which correspond to the atsign name and will assume
that these reside under ~/.atsign/keys. All of this behavior can be overridden with the 
following builder fields.

* **url** is used to set other root / directory endpoints, a proxy endpoint
  or the explicit endpoint for the at server.
* **keys** is used to explicitly provide the **AtKeys** which will be used for
  authentication, signing and encryption.
* **reconnect** is used to supply a **ReconnectStrategy** implementation that
  controls if reconnect is supported at all, how many retries and reconnections
  are supported, the pauses to apply between retries and when / if the at server
  endpoint should be re-resolved.
* **timeoutMillis** is used to specify the command timeout
* **queueLimit** is used to specify how many commands can be queued. By default
  the queue limit is zero.
* **awaitReadyMillis** is used to specify how long the builder blocks for the
  **AtCommandExecutor** to become ready (to send commands). NOTE: This will NOT
  trigger a build exception if the executor fails to become ready in that time, but
  the first command that is sent might be queued.

See the [examples](../examples) module.

---

## Command Line Utilities

The command line classes under
[src/main/java/org/atsign/client/cli](src/main/java/org/atsign/client/impl/cli)
serve as simple examples of how to instantiate an AtClient instance and invoke its
interface.

The following utilities provide a simple way to test behavior as-well as illustrating
the fundamentals of using the SDK.

**Note:** The example command lines use the **exec-maven-plugin** as this will handle
setting the classpath (see Dependencies section above).

**Note:** The example command lines which include vip.ve.atsign.zone:64 assume that
you are running the virtual environment (see section below).

### Share

The utility **org.atsign.client.cli.Share** can be used to share a value with
another AtSign. In this case, **@gary** is sharing the key value pair
(**message**, **hello**) with **@colin**.

```shell
mvn exec:java -Dexec.mainClass=org.atsign.client.cli.Share \
  -Dexec.args="vip.ve.atsign.zone:64 @gary @colin message hello"
```

### Get

The utility **org.atsign.client.cli.Get** can be used to get a value that has
been shared by another AtSign. In this case **@colin** is getting the value
for the key **message** which has been shared by **@gary**.

```shell
mvn exec:java -Dexec.mainClass=org.atsign.client.cli.Get \
  -Dexec.args="vip.ve.atsign.zone:64 @colin @gary message"
```

### Scan

The utility **org.atsign.client.cli.Scan** can be used to list keys that exist
at the AtServer of an AtSign. In this case **@gary** is listing the keys at his
own AtServer.

```shell
mvn exec:java -Dexec.mainClass=org.atsign.client.cli.Scan \
  -Dexec.args="vip.ve.atsign.zone:64 @gary .*"
```

### Delete

The utility **org.atsign.client.cli.Delete** can be used to delete a value that
was previously shared. In this case, **@gary** is deleting the key value for
**message** that was previously shared with **@colin**.

```shell
mvn exec:java -Dexec.mainClass=org.atsign.client.cli.Delete \
  -Dexec.args="vip.ve.atsign.zone:64 @gary @colin message"
```

### Register

The utility **org.atsign.client.cli.register.Register** can be used to perform
registration operations.

```shell
mvn exec:java -Dexec.mainClass=org.atsign.client.cli.register.Register -Dexec.args="--help"
```

When using the SUPER_API Key to register an atsign, the following sequence of
calls take place:

1. User provides at_java/Register with the SUPER_API Key passed as an argument
2. at_java calls the AtSign Registrar API* Endpoint(get-atsign) with the
   SUPER_API Key provided
3. The AtSign registrar API responds with an AtSign-ActivationKey pair
4. at_java now call the AtSign Registrar API* Endpoint(activate-atsign) with
   the AtSign-ActivationKey pair
5. The API responds with a json containing the CRAM_KEY* for the concerned
   atsign
6. This CRAM_KEY* can be used to activate the atsign further making it usable
7. at_java does the activation automatically for you and stores your atKeys*
   file at path '~/.atsign/keys'
8. Now the atsign is activated and the atKeys file can be used to
   authenticate and perform protected operation with/on the atSign.

#### Things to know about at_platform

1. Register: This is a class in at_java that has the functionality to call
   the necessary API, handle responses in order to fetch and register atsigns.
2. AtSign Registrar API: An AtSign service that is responsible for handling
   atsign's server creation, registration, authentication, reset and deletion.
3. SUPER_API Key
   All calls to the AtSign Registrar API require an API_KEY. But the
   SUPER_API Key has some additional privileges.
   SUPER_API Keys have the privilege to preset an AtSign with an activation
   key so that this AtSign can be activated without manually entering a
   verification code that is sent to the registered email.
   All SUPER_API Keys have a name containing two elements [say pre and
   post], all the atsigns generated using this API_Key will be of the
   following format: (pre)atsign(post). Now the atsign will be @preatsignpost.
   This is done to separate atsigns generated using SUPER_API Keys to the
   atsigns that are generated through other methods.
4. CRAM_KEY: This is an authentication key that will be used for a one-time
   authentication to activate an atsign which allows for assigning random,
   secure non-symmetric keypairs which will be further stored in the users
   atKeys file. **Note:** CRAM_KEY will be deleted from the atsign server after
   an atKeys file has been generated, so only you have the keys to authenticate
   into your atsign.
5. atKeys file: This will be a file generated during activation of an atsign
   that stores all the keys necessary for authenticating into atSign
   That would mean users have to keep this file in a secured location
   Users should keep this file safe, as there's only one copy of this file
   and losing it would mean the user would be unable to log in to the atsign.
   If lost, users can reset the atsign and get a new atKeys file. This
   would result in loss of all data stored in the atsign's server.

### Activate

The utility **org.atsign.client.cli.Activate** can be used to perform
onboarding and enrollment operations. Running using the **exec-maven-plugin**
will handle the classpath dependencies.

```shell
mvn exec:java -Dexec.mainClass=org.atsign.client.cli.Activate -Dexec.args="--help"
```

### DumpKeys

The utility **org.atsign.client.cli.DumpKeys** can be used to dumps the contents
of an AtSigns AtKeys. In this case **@gary**.

```shell
mvn exec:java -Dexec.mainClass=org.atsign.client.cli.DumpKeys -Dexec.args="@gary"
```
