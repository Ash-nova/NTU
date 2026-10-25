# EPRMS progress

Tracking list for the SOFT40051 build. The report checklist is a separate piece of writing.

## Core

- [x] C1 Projects and tasks
- [x] C2 Team members
- [x] C3 Search, filter, sort
- [x] C4 Next tasks with two strategies
- [x] C5 Statistics with lambdas and streams
- [x] C6 JavaFX interface with validation
- [x] C7 Persistence through repository interfaces
- [ ] C8 15+ JUnit 5 tests are in the project; the 20+ commits over six weeks are still yours to make on Olympus

## Extensions

- [x] E4 Task dependencies (topological sort and cycle detection)
- [x] E1 ORM persistence (JPA/Hibernate and H2, switched from `eprms.properties`)
- [ ] E2 TCP client-server — not attempted
- [ ] E3 UDP notifications — not attempted

## Session 20 October 2026

- Implemented: domain model, services, JSON and JPA repositories, JavaFX screens, dependency ordering, and the JUnit suite.
- Tests: `mvn clean test` — 47 tests, 0 failures (20 October 2026). `mvn javadoc:javadoc` completed for the domain and service layers.
- Commits: none from this session. Create the private Olympus repository and commit the work yourself, in small steps, across the weeks up to the deadline.
- Outstanding: your Olympus URL in the README, your own commit history, and the technical report.
- Next session: run the app, try the main screens, and make the first real commit if the tests are green.

## Suggested commits, when you are ready

Do not make these all on the same day. One idea per commit is enough:

1. `chore: initialise project from starter`
2. `feat(domain): add Task hierarchy, Project and TeamMember`
3. `test: cover domain rules`
4. `feat(service): add prioritisation strategies`
5. `feat(service): add search, filter, sort and next tasks`
6. `feat(service): add statistics with streams`
7. `test: cover strategies, search and statistics`
8. `feat(persistence): add repository interfaces and JSON storage`
9. `test: cover JSON round-trip and corrupt files`
10. `feat(ui): add project and task screens`
11. `feat(ui): add team, statistics and next-tasks screens`
12. `feat(service): add dependency ordering`
13. `feat(persistence): add JPA repositories behind the same interfaces`
14. `docs: describe how to build, run and switch storage`
