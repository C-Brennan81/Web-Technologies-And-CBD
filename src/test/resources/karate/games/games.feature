Feature: Games API

  Background:
    * url baseUrl
    * def username = 'karate_' + java.util.UUID.randomUUID()
    * def password = 'pass1234'

  # register
    Given path 'api', 'auth', 'register'
    And request { username: '#(username)', password: '#(password)' }
    When method post
    Then status 200

  # login + token
    Given path 'api', 'auth', 'login'
    And request { username: '#(username)', password: '#(password)' }
    When method post
    Then status 200
    * def token = response.token
    * configure headers = { Authorization: 'Bearer ' + token }

  Scenario: Get my games returns list
    Given path 'api', 'games'
    When method get
    Then status 200
    And match response == '#[]'