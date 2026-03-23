Feature: AtClient API tests for SelfKeys

  Background:
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is at_demo_data package lib/assets/atkeys
    And atsign keys suffix is .atKeys
    And verbose logging is off
    And AtClient for @gary

  Scenario: SelfKey get throws AtKeyNotFoundException if no key
    Then AtClient.get fails for SelfKey test
    And exception was AtKeyNotFoundException and message matches "does not exist in keystore"

  Scenario: SelfKey get returns value from put
    When AtClient.put for SelfKey test and value "hello world"
    Then AtClient.get for SelfKey test returns value that matches "hello world"

  Scenario: SelfKey get throws AtKeyNotFoundException if key is deleted
    And AtClient.put for SelfKey test and value "hello world"
    And AtClient.get for SelfKey test returns value that matches "hello world"
    When AtClient.delete for SelfKey test
    Then AtClient.get fails for SelfKey test
    And exception was AtKeyNotFoundException and message matches "does not exist in keystore"

  Scenario: SelfKeys are visible to owner
    When AtClient.put for SelfKey test and value "hello world"
    Then AtClient.getAtKeys for ".+" contains
      | test@gary |
    And AtClient.getAtKeys for "test.+" contains
      | test@gary |

  Scenario: SelfKeys are invisible to other at signs
    And AtClient.put for SelfKey test and value "hello world"
    Then @colin AtClient.getAtKeys for ".+" does NOT contain
      | test@gary |

  Scenario: SelfKey put bytes encodes as Base15e2
    When AtClient.put for SelfKey test and bytes
      | Binary   |          |          |          |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 |          |
    Then AtClient.get for SelfKey test returns value that matches "곹彴곹彴곹彴곹彴"
    But AtClient.getBinary for SelfKey test returns bytes that matches
      | Binary   |          |          |          |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 | 10101010 |
      | 10101010 | 10101010 | 10101010 |          |

