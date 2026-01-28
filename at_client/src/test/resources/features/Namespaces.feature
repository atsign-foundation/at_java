Feature: AtClient API tests for namespaces atsign

  Background:
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is at_demo_data package lib/assets/atkeys
    And atsign keys suffix is .atKeys
    And verbose logging is off
    And @srie Activate.onboard with SrieKeys._cramKey from at_demo_apkam_keys.dart in at_demo_data package

  Scenario: Test namespace access control
    And @srie Activate.otp generates an OTP
    And @srie Activate.enroll for app app1 and device device1 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns1       | rw             |
    And @srie Activate.approve for last enrollment
    And AtClient with keys @srie-app1-device1.atKeys for @srie completes enrollment
    When AtClient.put for PublicKey test.ns1 and value "hello ns1 world"
    Then AtClient.get for PublicKey test.ns1 returns value that matches "hello ns1 world"
    And AtClient.getAtKeys for "test" contains
      | Key                  | Name | Namespace | Shared By | Shared With |
      | public:test.ns1@srie | test | ns1       | srie      |             |
    When namespace is set to ns2
    Then AtClient.put fails for PublicKey test and value "hello ns2 world"
    And exception was AtUnauthorizedException and message matches "not authorized to update key"

  Scenario: Test namespace read-only access control
    And @srie Activate.otp generates 2 OTPs
    And @srie Activate.enroll for app app1 and device readwrite with last OTP and following namespaces
      | Namespace | Access Control |
      | ns        | rw             |
    And @srie Activate.enroll for app app1 and device readonly with last OTP and following namespaces
      | Namespace | Access Control |
      | ns        | r              |
    And @srie Activate.approve for all enrollments
    And AtClient with keys @srie-app1-readwrite.atKeys for @srie completes enrollment
    And AtClient with keys @srie-app1-readonly.atKeys for @srie completes enrollment
    When 1st @srie AtClient.put for PublicKey test.ns and value "hello ns world"
    Then 2nd @srie AtClient.get for PublicKey test.ns returns value that matches "hello ns world"
    But 2nd @srie AtClient.put fails for PublicKey test.ns and value "hello ns world"
    And exception was AtUnauthorizedException and message matches "not authorized to update key"

  Scenario: Test namespace read visibility of SelfKeys
    And @srie Activate.otp generates 2 OTPs
    And @srie Activate.enroll for app app1 and device device1 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns1       | rw             |
    And @srie Activate.enroll for app app1 and device device2 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns2       | rw             |
      | ns1       | r              |
    And @srie Activate.approve for all enrollments
    When AtClient with keys @srie-app1-device1.atKeys for @srie completes enrollment
    And AtClient with keys @srie-app1-device2.atKeys for @srie completes enrollment
    And 1st @srie AtClient.put for SelfKey test.ns1 and value "test data 1"
    And 2nd @srie AtClient.put for SelfKey test.ns2 and value "test data 2"
    Then 1st @srie AtClient.getAtKeys for ".*" contains
      | Key           | Name | Namespace | Shared By |
      | test.ns1@srie | test | ns1       | srie      |
    And 1st @srie AtClient.getAtKeys for ".*" does NOT contain
      | Key           |
      | test.ns2@srie |
    But 2nd @srie AtClient.getAtKeys for ".*" contains
      | Key           | Name | Namespace | Shared By |
      | test.ns1@srie | test | ns1       | srie      |
      | test.ns2@srie | test | ns2       | srie      |
    And 2nd @srie AtClient.get for SelfKey test.ns1 returns value that matches "test data 1"
    And 2nd @srie AtClient.put fails for SelfKey test.ns1 and value "xxx"

  Scenario: Test namespace read visibility of SharedKeys
    And @srie Activate.otp generates 2 OTPs
    And @srie Activate.enroll for app app1 and device device1 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns1       | rw             |
    And @srie Activate.enroll for app app1 and device device2 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns2       | rw             |
      | ns1       | r              |
    And @srie Activate.approve for all enrollments
    When AtClient with keys @srie-app1-device1.atKeys for @srie completes enrollment
    And AtClient with keys @srie-app1-device2.atKeys for @srie completes enrollment
    And 1st @srie AtClient.put for SharedKey test.ns1 shared with @colin and value "test data 1"
    And 2nd @srie AtClient.put for SharedKey test.ns2 shared with @colin and value "test data 2"
    Then 1st @srie AtClient.getAtKeys for ".*" contains
      | Key                  | Name | Namespace | Shared By | Shared With |
      | @colin:test.ns1@srie | test | ns1       | srie      | colin       |
    And 1st @srie AtClient.getAtKeys for ".*" does NOT contain
      | Key                  |
      | @colin:test.ns2@srie |
    But 2nd @srie AtClient.getAtKeys for ".*" contains
      | Key                  | Name | Namespace | Shared By | Shared With |
      | @colin:test.ns1@srie | test | ns1       | srie      | colin       |
      | @colin:test.ns2@srie | test | ns2       | srie      | colin       |

  Scenario: Test multiple app / devices updating the same shared key
    And @srie Activate.otp generates 2 OTPs
    And @srie Activate.enroll for app app1 and device device1 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns1       | rw             |
    And @srie Activate.enroll for app app1 and device device2 with last OTP and following namespaces
      | Namespace | Access Control |
      | ns1       | rw             |
    And @srie Activate.approve for all enrollments
    And AtClient with keys @srie-app1-device1.atKeys for @srie completes enrollment
    And AtClient with keys @srie-app1-device2.atKeys for @srie completes enrollment
    When 1st @srie AtClient.put for SharedKey test.ns1 shared with @colin and value "test data 1"
    Then @colin AtClient.get for SharedKey test.ns1 shared by @srie returns value that matches "test data 1"
    When 2nd @srie AtClient.put for SharedKey test.ns1 shared with @colin and value "test data 2"
    Then @colin AtClient.get for SharedKey test.ns1 shared by @srie returns value that matches "test data 2"
