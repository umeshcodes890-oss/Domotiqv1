# DOMOTIQ Documentation

Documentation set for the DOMOTIQ Smart Home Automation System (Java Swing +
MySQL/JDBC desktop application).

## Contents

| Document | What it covers |
|----------|----------------|
| [project-description.md](project-description.md) | Full project description: goals, tech stack, feature specification, OO architecture, database design, security model, module map, and how to extend device categories |

## Quick start

```bash
# 1. create the schema
mysql -u root -p < ../database/init.sql

# 2. configure credentials (never committed)
cp ../config/domotiq.properties.example ../config/domotiq.properties
#    then edit db.user / db.password

# 3. build and run from the project root
mvn package
java -jar ../target/domotiq-1.0.0.jar
```

The working directory matters: the app loads `config/domotiq.properties`
relative to the folder it is launched from.

## Related files in the repository

- `../README.md` — repository overview and setup guide
- `../database/init.sql` — MySQL schema (5 tables, FKs, constraints)
- `../src/main/java/com/domotiq/` — source tree (see module map in the
  project description)

## Note on device simulation

All IoT behaviour in v1 is generated in software (power state, settings,
connectivity, sensor telemetry). Nothing in this project communicates with
physical hardware. See *Architecture → DeviceSimulator* in the project
description for where a real transport layer would integrate.
