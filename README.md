# EPRMS

Enterprise Project and Resource Management System for SOFT40051. A project manager records projects, breaks them into feature and bug tasks, assigns those tasks to team members, and sees what needs attention. There is one user and no login.

## Build and test

From this folder:

```
mvn clean test
```

That needs JDK 17 or newer and Maven. The first run downloads the libraries.

## Run the application

```
mvn javafx:run
```

Always start `uk.ac.ntu.eprms.Launcher`. Running `App` directly misses the JavaFX runtime.

## Where data is stored

By default the app writes:

- `data/projects.json`
- `data/members.json`

Those files are created on the first save. A missing file, or a file that is not valid JSON, loads as an empty list and is logged. The application does not crash.

To use the embedded H2 database instead (extension E1), edit `eprms.properties` in this folder:

```
persistence=jpa
```

Or start the JVM with `-Deprms.persistence=jpa`. The database file is `eprms-db.mv.db`. Switching mode does not require a change in the window or the service classes. `RepositoryFactory` is the only place that chooses the implementation.

Set `persistence=json` to go back to files.

## Olympus repository

Add the private Olympus URL here once the repository exists:

```
[ADD YOUR OLYMPUS REPOSITORY URL]
```

Commit regularly, with a message that says what changed and why. The brief asks for at least 20 commits spread over at least six different weeks. Do not commit `target/`, `data/`, log files, or the H2 database files. `.gitignore` already excludes them.

## Layout

| Folder | What belongs there |
|---|---|
| `domain` | Project, Task, FeatureTask, BugTask, TeamMember |
| `service` | Search, filter, sort, statistics, prioritisation strategies, dependencies |
| `persistence` | Repository interfaces, JSON implementations, JPA implementations |
| `ui` | JavaFX screens |

The window calls services. It does not read files itself. Load and save run on a background thread.
