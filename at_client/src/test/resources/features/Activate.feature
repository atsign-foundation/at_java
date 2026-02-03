Feature: AtClient API tests for onboarding and enrolling atsign

  Background:
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is at_demo_data package lib/assets/atkeys
    And atsign keys suffix is .atKeys
    And verbose logging is off

  Scenario: Attempt to create AtClient prior to onboarding fails
    When AtClient fails for @device2
    Then exception message matches "privatekey:at_pkam_publickey does not exist in keystore"

  Scenario: Attempt to onboard atsign with incorrect CRAM secret fails
    When @srie Activate.onboard fails with CRAM secret "not-the-correct-cramkey"
    Then exception was AtUnauthenticatedException and message matches "Authentication Failed"

  Scenario: After onboarding an AtClient can be created and used to get and put keys
    When @srie Activate.onboard with SrieKeys._cramKey from at_demo_apkam_keys.dart in at_demo_data package
    Then AtClient with keys @srie.atKeys for @srie
    And AtClient.getAtKeys for ".+" contains
      | Key                           | Name               | Namespace | Shared By | Shared With | Is Public | Is Encrypted | Is Hidden |
      | @srie:signing_privatekey@srie | signing_privatekey |           | srie      | srie        | false     | true         | false     |
      | public:publickey@srie         | publickey          |           | srie      |             | true      | false        | false     |
      | public:signing_publickey@srie | signing_publickey  |           | srie      |             | true      | false        | false     |
    When AtClient.put for PublicKey test and value "hello world"
    And AtClient.put for SelfKey test and value "hello me"
    And AtClient.put for SharedKey test shared with @colin and value "hello colin"
    Then AtClient.getAtKeys for ".+" contains
      | Key              | Name | Namespace | Shared By | Shared With |
      | @colin:test@srie | test |           | srie      | colin       |
      | public:test@srie | test |           | srie      |             |
      | test@srie        | test |           | srie      |             |
    And AtClient.get for PublicKey test returns value that matches "hello world"
    And AtClient.get for SelfKey test returns value that matches "hello me"
    And @colin AtClient.get for PublicKey test shared by @srie returns value that matches "hello world"
    And @colin AtClient.get for SharedKey test shared by @srie returns value that matches "hello colin"
    And @gary AtClient.get for PublicKey test shared by @srie returns value that matches "hello world"

  Scenario: After enrolling an app and device but prior to approval an AtClient cannot be created
    When @srie Activate.onboard with SrieKeys._cramKey from at_demo_apkam_keys.dart in at_demo_data package
    And @srie Activate.otp generates an OTP
    And @srie Activate.enroll for app app1 and device device1 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns        | rw             |
    Then AtClient with keys @srie-app1-device1.atKeys fails for @srie
    And exception message matches "PKAM command failed: error:AT0026:enrollment_id: .+ is pending"

  Scenario: After enrolling an app and device and approving an AtClient can be created and used to get and put keys
    When @srie Activate.onboard with SrieKeys._cramKey from at_demo_apkam_keys.dart in at_demo_data package
    And @srie Activate.otp generates an OTP
    And @srie Activate.enroll for app app1 and device device1 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns        | rw             |
    And @srie Activate.approve for last enrollment
    And AtClient with keys @srie-app1-device1.atKeys for @srie completes enrollment
    And AtClient.put for SelfKey test.ns and value "hello me"
    And AtClient.put for PublicKey test.ns and value "hello world"
    And AtClient.put for SharedKey test.ns shared with @colin and value "hello colin"
    Then AtClient.getAtKeys for "test" contains
      | Key                 | Name | Namespace | Shared By | Shared With |
      | test.ns@srie        | test | ns        | srie      |             |
      | public:test.ns@srie | test | ns        | srie      |             |
      | @colin:test.ns@srie | test | ns        | srie      | colin       |
    And AtClient.get for PublicKey test.ns returns value that matches "hello world"
    And AtClient.get for SelfKey test.ns returns value that matches "hello me"
    And @colin AtClient.get for PublicKey test.ns shared by @srie returns value that matches "hello world"
    And @colin AtClient.get for SharedKey test.ns shared by @srie returns value that matches "hello colin"
    And @gary AtClient.get for PublicKey test.ns shared by @srie returns value that matches "hello world"

  Scenario: After enrolling an app and device and denying an AtClient cannot be created
    And @srie Activate.onboard with SrieKeys._cramKey from at_demo_apkam_keys.dart in at_demo_data package
    And @srie Activate.otp generates an OTP
    And @srie Activate.enroll for app app1 and device device1 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns        | rw             |
    When @srie Activate.deny for last enrollment
    Then AtClient with keys @srie-app1-device1.atKeys fails for @srie
    And exception message matches "PKAM command failed: error:AT0025:enrollment_id: .+ is denied"

  Scenario: After enrolling an app and device and approving but then revoking an AtClient cannot be created
    And @srie Activate.onboard with SrieKeys._cramKey from at_demo_apkam_keys.dart in at_demo_data package
    And @srie Activate.otp generates an OTP
    And @srie Activate.enroll for app app1 and device device1 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns        | rw             |
    And @srie Activate.approve for last enrollment
    And AtClient with keys @srie-app1-device1.atKeys for @srie completes enrollment
    And AtClient.put for PublicKey test.ns and value "hello world"
    When @srie Activate.revoke for last enrollment
    Then AtClient.get fails for PublicKey test.ns
    And AtClient with keys @srie-app1-device1.atKeys fails for @srie
    When AtClient is closed
    And @srie Activate.unrevoke for last enrollment
    And AtClient with keys @srie-app1-device1.atKeys for @srie
    Then AtClient.get for PublicKey test.ns returns value that matches "hello world"

