Feature: Auth API basic responses

  Background:
    * url baseUrl
    * configure headers = { 'Content-Type': 'application/json' }

  Scenario: register requires username and password
    Given path '/api/auth/register'
    And request { username: '', password: null }
    When method post
    Then status 400
    And match response contains 'Username and password are required'

  Scenario: login returns 401 on bad credentials
    Given path '/api/auth/login'
    And request { username: 'u', password: 'p' }
    When method post
    Then status 401
    And match response contains 'Invalid credentials'

  Scenario: protected endpoint without token returns 401
    Given path '/api/games'
    When method get
    Then status 401
