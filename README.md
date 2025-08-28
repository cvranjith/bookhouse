```text
📚📗📒📙📘📕📚📖📓📕📋📝📚📗📒📙📕📚📖📓📕📋📝📕📘
 ____              _    _   _                      
| __ )  ___   ___ | | _| | | | ___  _   _ ___  ___ 
|  _ \ / _ \ / _ \| |/ / |_| |/ _ \| | | / __|/ _ \
| |_) | (_) | (_) |   <|  _  | (_) | |_| \__ \  __/
|____/ \___/ \___/|_|\_\_| |_|\___/ \__,_|___/\___|

📒📖📓📕📝📚📗📒📙📘📕📚📒📙📘📗📙📘📕📚📖📓📕📋📝
```

# Bookhouse CLI
Welcome to the Bookhouse CLI Library App

> **TL;DR**: Bookhouse is an interactive **CLI library manager** App built using Spring Boot + Spring Shell where users can log in, list/borrow/return books, join waitlists, and view their status, with **RBAC, audit logging, and extensible UX/configuration**.


## Quick links
- 📋 [Requirements (as received)](docs/requirements.md)
- 📓 [User Manual](docs/user_manual.md)
- 🏗️ [Architecture](docs/design_documentation.md)

## Quickstart
- Prereqs: <JDK 17 or Docker>
- Run: `sh start.sh`
- Tests: `./mvnw test`
        `sh src/test/cli/integration-test.sh`

## Notes for Reviewers

All base requirements from `requirements.md` have been implemented, with several thoughtful extensions (RBAC, audit logging, reports, copies, UX polish).  
Where command names differ slightly, functional parity is maintained and documented in the design overview.
