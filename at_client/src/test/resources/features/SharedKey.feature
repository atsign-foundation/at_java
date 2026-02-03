Feature: AtClient API test for SharedKeys

  Background:
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is target/at_demo_data/lib/assets/atkeys
    And atsign keys suffix is .atKeys
    And verbose logging is off
    And AtClient for @gary

  Scenario: SharedKey get throws AtKeyNotFoundException if no "Shared With" key
    Then AtClient.get fails for SharedKey test shared with @colin
    And exception was AtKeyNotFoundException and message matches "does not exist in keystore"

  Scenario: SharedKey get throws AtKeyNotFoundException if no "Shared By" key
    Then @colin AtClient.get fails for SharedKey test shared by @gary
    And exception was AtKeyNotFoundException and message matches "does not exist in keystore"

  Scenario: SharedKey get throws AtKeyNotFoundException if no "Shared By" key but other keys exist
    And AtClient.put for SharedKey test shared with @colin and value "hello world"
    Then @colin AtClient.get fails for SharedKey test2 shared by @gary
    And exception was AtKeyNotFoundException and message matches "does not exist in keystore"

  Scenario: SharedKey get returns expected value for "Shared By" and "Shared With" atsigns
    When AtClient.put for SharedKey test shared with @colin and value "hello world"
    Then @gary AtClient.get for SharedKey test shared with @colin returns value that matches "hello world"
    And @colin AtClient.get for SharedKey test shared by @gary returns value that matches "hello world"

  Scenario: SharedKey get throws AtKeyNotFoundException for "Shared By" and "Shared With" atsigns if key is deleted
    And AtClient.put for SharedKey test shared with @colin and value "hello world"
    And AtClient.get for SharedKey test shared with @colin returns value that matches "hello world"
    When AtClient.delete for SharedKey test shared with @colin
    Then @gary AtClient.get fails for SharedKey test shared with @colin
    And exception was AtKeyNotFoundException and message matches "test@gary does not exist in keystore"
    And @colin AtClient.get fails for SharedKey test shared by @gary
    And exception was AtKeyNotFoundException and message matches "test@gary does not exist in keystore"

  Scenario: SharedKeys are visible to owner
    When AtClient.put for SharedKey test shared with @colin and value "hello world"
    Then AtClient.getAtKeys for ".+" contains
      | @colin:test@gary |
    And AtClient.getAtKeys for "test.+" contains
      | @colin:test@gary |

  Scenario: SharedKeys are not visible to other at signs
    And AtClient.put for SharedKey test shared with @colin and value "hello world"
    Then @don AtClient.getAtKeys for ".+" does NOT contain
      | public:test@gary |

  Scenario: Namespace qualified
      When @gary AtClient.put for SharedKey message.ns shared with @colin and value "hi colin it's gary"
      And @colin AtClient.put for SharedKey message.ns shared with @gary and value "hi gary it's colin"
      Then @colin AtClient.get for SharedKey message.ns shared by @gary returns value that matches "hi colin it's gary"
      And @gary AtClient.get for SharedKey message.ns shared by @colin returns value that matches "hi gary it's colin"
