Feature: Auth API

  Background:
    * url baseUrl
    * def username = 'karate_' + java.util.UUID.randomUUID()
    * def password = 'pass1234'

  Scenario: Register then login returns JWT
    Given path 'api', 'auth', 'register'
    And request { username: '#(username)', password: '#(password)' }
    When method post
    Then status 200
    And match response == 'Registered'

    Given path 'api', 'auth', 'login'
    And request { username: '#(username)', password: '#(password)' }
    When method post
    Then status 200
    And match response.token == '#string'