# Feedback

Thank you for trying out FlowTracker and considering contributing.

You may report issues on https://github.com/coekie/flowtracker/issues .
This is an experimental, not actively supported project.
Please set your expectations accordingly.

Reports of crashes or other incorrect behaviour that include minimal reproducing code would be appreciated.
Feature requests or more general "FlowTracker does not track X in application Y" reports are unlikely to get addressed at this stage.

Other feedback is also welcome, see my contact details on https://wouter.coekaerts.be/about .

# Development Setup

## Prerequisites to build FlowTracker

* npm (see https://github.com/nvm-sh/nvm)
* node (`nvm install node`)
* maven

Tests are ran on a set of JDK versions, by submodules in the `test` directory.
To be able to run those, you must install those JDKs, and make maven aware of where they are located, in your `~/.m2/toolchains.xml`.
As an example, here is [my toolchains.xml](https://gist.github.com/coekie/13cb03a2db1c78f296f87b1530356a49).

When making changes to `core` or `weaver`, it's best to actually run the tests against all those JDKs.
That's because we're not just _using_ the JDK, we're modifying its behaviour, so all compatibility expectations are out the window.
If installing all those JDKs bothers you, then you can temporarily comment out the references to those jdks (e.g. the `<module>test/test-jdk22</module>` line) in the root pom.xml, and ask me to improve that setup.

## Building FlowTracker

To build the project and run all tests: `mvn verify`.
Note that this also causes maven to invoke `npm` to build the ui; that part can be skipped with `-Pskip-ui`.

To generate the snapshots of the demos that are also hosted on https://flowtracker-demo.coekie.com/ and linked from our README:
Make sure you are in an (initially) empty directory, and run `.../demo/generate-demos.sh`.

## Agent Development

The full build process generates the full flowtracker jar (including dependencies) at `flowtracker/target/flowtracker-*-SNAPSHOT.jar`.
If you want to test your changes against another application (or the FlowTracker demos), you _could_ use that jar.
But rebuilding it every time you make a change is relatively slow.
To iterate more quickly, you can just recompile (`mvn compile -Pskip-ui`) and then use the development agent in `agent/agent-dev/target/agent-dev-*-SNAPSHOT.jar`.
That agent uses classes directly from the `target` directories.
That is also what the unit tests use.

Tip: In IntelliJ, in a Run Configuration, you can add a "task" to "Run Maven Goal" `compile -Pskip-ui`, and in "Add VM options" add `-javaagent:agent/agent-dev/target/agent-dev-...-SNAPSHOT.jar ...` (see USAGE.md).

## UI Development

In the `ui` directory run `npm run dev`.
That starts Vite, configured to proxy requests to localhost:8081, where the flowtracker agent listens by default.
That way you can iterate on the ui, with working Hot Module Replacement.

You can run only the ui tests with `npm test` or `npm run test:watch` or `npm run test:ui`.
Use `npm run verify` to run linting and tests (this is also what `mvn verify` will run).
