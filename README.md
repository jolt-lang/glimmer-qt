# glimmer-qt

> **Status:** Recently extracted from Snitt. The repository still needs cleanup.

A Qt Quick backend for Glimmer on Jolt. See [EXAMPLES.md](EXAMPLES.md) to get started.

Build the native bridge with `./build-native`. Run tests with:

```sh
jolt test                # all automated suites
jolt test lifecycle      # one suite
jolt test --list         # available suites
```

`./bin/test` provides the same interface. It replaces the individual root
`test-*` scripts and handles building, Qt setup, fixtures, and process deadlines.
See [TESTING.md](TESTING.md) for dependencies and desktop checks.
