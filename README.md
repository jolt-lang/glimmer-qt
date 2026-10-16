# glimmer-qt

> **Status:** Recently extracted from Snitt. The repository still needs cleanup.
> The vendored Glimmer is upstream main plus two pull requests awaiting review,
> [#12](https://github.com/jolt-lang/glimmer/pull/12) and
> [#13](https://github.com/jolt-lang/glimmer/pull/13) (see
> [LIFECYCLE.md](LIFECYCLE.md#explicit-glimmer-contract-extension)); the
> vendored copy goes away once they land.

A Qt Quick backend for Glimmer on Jolt. See [EXAMPLES.md](EXAMPLES.md) to get started.

Build the native bridge with `./build-native`. Run tests with:

```sh
jolt test                # all automated suites, including vendored Glimmer
jolt test lifecycle      # one suite
jolt test --list         # available suites
```

`./bin/test` provides the same interface. It replaces the individual root
`test-*` scripts and handles building, Qt setup, fixtures, and process deadlines.
See [TESTING.md](TESTING.md) for dependencies and desktop checks.
