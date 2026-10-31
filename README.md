# EPRMS

SOFT40051 coursework. This is a small desktop app for managing projects, feature/bug tasks, and team members. There is a single user and no login screen.

Student: Ashwin Babu Iyer (N1494935)

## Olympus repository

https://olympus.ntu.ac.uk/N1494935/ASEPROJECT_N1494935

## Build and test

Needs JDK 17+ and Maven. From this folder:

```
mvn clean test
```

## Run

```
mvn javafx:run
```

Start through `uk.ac.ntu.eprms.Launcher`. Do not run `App` on its own or JavaFX will not load correctly.

## Data storage

Default mode is JSON files under `data/`:

- `data/projects.json`
- `data/members.json`

If a file is missing or invalid, the app loads an empty list and logs the problem instead of crashing.

To use the embedded H2 database (extension E1), set this in `eprms.properties`:

```
persistence=jpa
```

You can also pass `-Deprms.persistence=jpa`. The database file is `eprms-db.mv.db`. Switch back with `persistence=json`. The UI and services stay the same; `RepositoryFactory` picks the storage implementation.

Do not commit `target/`, `data/`, log files, or H2 database files. Those are already listed in `.gitignore`.

## Project layout

| Package | Contents |
|---|---|
| `domain` | Project, Task, FeatureTask, BugTask, TeamMember |
| `service` | Search, filter, sort, statistics, prioritisation, dependencies |
| `persistence` | Repository interfaces, JSON storage, JPA storage |
| `ui` | JavaFX screens |

The UI talks to services only. Load and save run on a background thread.

## What is included

Core C1–C8, plus extensions:

- E4 — task dependencies with ordering and cycle checks
- E1 — JPA/Hibernate + H2 behind the same repository interfaces
