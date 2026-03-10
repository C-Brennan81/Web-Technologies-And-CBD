# Web-Technologies-And-CBD

This project now includes a complete test suite following the testing pyramid and API tests using Karate.

Testing pyramid applied here:
- Unit tests (base): focus on small units such as services and utility classes.
- Web layer tests (middle): controller slice tests using MockMvc.
- Integration/API (top/thin): black-box API scenarios with Karate running against an embedded Spring Boot server on a random port.

What’s covered
- Unit tests
  - `JwtServiceTest` validates JWT generation/validation and username extraction.
  - `GameServiceTest` covers CSV import logic (happy path + skips) and DTO mapping.
- Web layer tests
  - `AuthControllerTest` covers register/login happy/negative paths using MockMvc with mocked collaborators.
- Karate API tests
  - `src/test/java/karate/auth/auth.feature` covers basic `/api/auth` negative flows (bad registration payload, invalid login). The runner starts the Spring app on a random port and hits real HTTP endpoints.

How to run tests
- Run all tests:
  - `mvn -q -DskipITs=false test`
- Run only unit + web layer (JUnit) tests:
  - `mvn -q -Dtest='com.gamelibrary.Tests.**.*Test' test`
- Run only Karate tests:
  - `mvn -q -Dtest='com.gamelibrary.Tests.karate.AuthKarateRunner' test`

Test configuration
- Uses a `test` Spring profile with in-memory H2 database.
- Test properties: `src/test/resources/application-test.properties` defines the JWT secret and H2 config.

Code coverage (JaCoCo)
- Generate the JaCoCo site report:
  - `mvn -q clean test jacoco:report`
- Open the HTML report at: `target/site/jacoco/index.html`.

Notes
- The Karate base URL is configured via `karate-config.js` and the JUnit runner sets `baseUrl` to the random port at runtime.
- If you prefer to run Karate against an already-running server, set `-DbaseUrl=http://localhost:8080` and run the Karate runner tests; the runner can be adapted accordingly.
