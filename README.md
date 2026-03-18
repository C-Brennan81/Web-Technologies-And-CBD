# Game Library Stats — Unified Game Library Manager

Game Library Stats is a simple web app that helps you manage your entire games collection in one place — across PC launchers and stores like Steam, Epic, GOG, Origin/EA, Ubisoft Connect, itch.io, and more. It lets you import your library, view beautiful cover art, search and filter, and see quick stats about your collection — all from a single dashboard.

This project was built with students and testers in mind: it has a clean Spring Boot backend, a lightweight web UI, and a solid automated test suite.

## What you can do
- Keep one unified view of all your games regardless of where you bought or installed them
- Import your library from CSV (with a provided template) or extend to import from store exports
- Search, filter, and browse your collection with cover art and metadata
- See high‑level stats (e.g., total games, completion %) and track progress over time
- Secure your data with accounts and JWT‑based auth
- Optional: Launch locally installed games via the built‑in launcher hooks (where supported)

## How it works
- Backend: Spring Boot application exposing a REST API for authentication, games, import, and stats
- Frontend: A lightweight client served from `src/main/resources/static` (no heavy framework required to run)
- Storage: H2 (in‑memory) by default for local dev/tests; easily replaceable with another database
- Media: Cover art fetched from SteamGridDB to make your library look great

Key packages and tests (examples):
- Auth and security: JWT services and filters (see `JwtServiceTest`)
- Game import and business logic: CSV parsing, de‑duplication, and mapping (see `GameServiceTest`)
- API and controller layer: controller slice tests and integration tests
- UI smoke test: Selenium verifies registration, login, and CSV import flows (`UISmokeIT`)

## Supported sources
Out of the box, CSV import is supported. You can export games from various launchers/stores and map them to the provided template. There is a ready‑to‑use template CSV in the repo:
- `src/main/resources/static/csv/template.csv`

The template includes common columns such as: title, platform/store, status/completion, hours played, price, and notes. You can add or ignore optional columns — the importer tolerates extra fields and will skip unknowns when reasonable.

## Quick start
Prerequisites:
- Java 17+
- Maven 3.9+

Run the app:
1. Clone the repository
2. From the project root, run:
   - `mvn spring-boot:run`
3. Open the web app:
   - `http://localhost:8081/`

That’s it — the app serves both API and UI. You can register a user, log in, and start importing your games.

Tip: A sample/template CSV is at `src/main/resources/static/csv/template.csv`.

## Importing your library (CSV)
1. Go to the Dashboard after logging in
2. Click Import and choose your CSV file
3. Confirm the import summary
4. Your stats and game grid/table will update immediately

Importer behavior:
- Skips rows with missing required fields
- De‑duplicates known titles by source when possible
- Ignores empty/unknown optional columns gracefully
- Provides a clear result message after import

## Cover art via SteamGridDB
The app can fetch cover images from SteamGridDB to enhance your library view. In most cases it works anonymously, but if you hit rate limits you can provide an API key via environment variable or application properties:
- `STEAMGRIDDB_API_KEY=your_key_here`

## Authentication and security
- Registration and login are handled by the backend
- JWT tokens secure subsequent API requests
- Tests ensure common happy/negative paths work as expected

## Configuration
Most defaults work out-of-the-box. Common tweaks:
- Server port and profiles via standard Spring properties
- Test profile (`test`) uses in‑memory H2 and dedicated JWT settings

## Run the tests
- All tests:
  - `mvn -q -DskipITs=false test`
- Only unit + web layer (JUnit):
  - `mvn -q -Dtest=com.gamelibrary.Tests.**.*Test test`
- Only Karate API tests:
  - `mvn -q -Dtest=com.gamelibrary.Tests.karate.AuthKarateRunner test`

Generate code coverage (JaCoCo):
- `mvn -q clean test jacoco:report`
- Open `target/site/jacoco/index.html`

## Tech stack
- Java 17, Spring Boot
- H2 (dev/test), JPA
- Selenium (UI smoke), JUnit 5, Karate (API), JaCoCo
- Static web UI (HTML/CSS/JS) served by Spring Boot

## Why this exists
Managing a games collection spread across Steam, Epic, GOG, and others is painful. This project gives you a central, privacy‑friendly place to keep track of everything you own, with just enough automation (CSV import, cover art, stats) to make it delightful without being complex.

## Roadmap ideas
- Native importers for Steam/Epic/GOG exports
- Cloud database option and multi‑device sync
- Richer stats, completion tracking, and backlog helpers
- Enhanced launcher integration on Windows/macOS/Linux

—
If you run into issues, please open an issue or reach out with details about your CSV, platform versions, and logs located under `logs/`.
