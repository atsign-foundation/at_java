Feature: AtClient API tests for Keys

  Scenario: Test
    Given root server endpoint is vip.ve.atsign.zone:64
    And root server is running
    And atsign keys path is target/at_demo_data/lib/assets/atkeys
    And atsign keys suffix is .atKeys
    When AtClient for gary
    Then AtClient.getAtKeys for ".+" contains
      | Key                           | Name               | Namespace | Shared By | Shared With | Is Public | Is Encrypted | Is Hidden |
      | @gary:signing_privatekey@gary | signing_privatekey |           | gary      | gary        | false     | true         | false     |
      | public:pkaminstalled@gary     | pkaminstalled      |           | gary      |             | true      | false        | false     |
      | public:publickey@gary         | publickey          |           | gary      |             | true      | false        | false     |
      | public:signing_publickey@gary | signing_publickey  |           | gary      |             | true      | false        | false     |
