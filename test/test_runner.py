"""Runner failure-path checks; run with python3 -m unittest discover -s test."""

from contextlib import redirect_stdout, redirect_stderr
from importlib.machinery import SourceFileLoader
from importlib.util import module_from_spec, spec_from_loader
import io
import os
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "bin"))
loader = SourceFileLoader("qt_test_runner", str(ROOT / "bin/test"))
runner = module_from_spec(spec_from_loader(loader.name, loader))
loader.exec_module(runner)


class RunnerTests(unittest.TestCase):
    def test_help_and_list_do_not_require_toolchain(self):
        env = dict(os.environ, JOLT="/missing/jolt", GLIMMER_QT_QMAKE="/missing/qmake")
        for argument in ("--help", "--list"):
            result = subprocess.run([sys.executable, str(ROOT / "bin/test"), argument],
                                    env=env, capture_output=True, text=True, timeout=10)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("macos-activation", result.stdout)

    def test_all_continues_after_failure_and_timeout(self):
        visited = []

        def suite(name, *args):
            visited.append(name)
            if name == "layout":
                raise subprocess.TimeoutExpired("fake-jolt", 60)
            return 7 if name == "values" else 0

        with patch.object(sys, "argv", ["test", "all"]), \
             patch.object(runner, "jolt", return_value="fake-jolt"), \
             patch.object(runner, "build_environment", return_value={}) as build, \
             patch.object(runner, "run_suite", side_effect=suite), \
             redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()) as errors:
            self.assertEqual(runner.main(), 1)
        self.assertIn("glimmer", visited)
        self.assertNotIn("macos-activation", visited)
        self.assertEqual(len(visited), 16)
        self.assertEqual([call.args for call in build.call_args_list], [(False,), (True,)])
        self.assertIn("Failed: layout, values", errors.getvalue())

    def test_failed_build_never_starts_a_suite(self):
        with patch.object(sys, "argv", ["test", "values"]), \
             patch.object(runner, "jolt", return_value="fake-jolt"), \
             patch.object(runner, "build_environment", side_effect=subprocess.CalledProcessError(2, "make")), \
             patch.object(runner, "run_suite") as suite, redirect_stdout(io.StringIO()):
            self.assertEqual(runner.cli(runner.main), 2)
            suite.assert_not_called()

    def test_timeout_preserves_child_diagnostics(self):
        command = [sys.executable, "-u", "-c",
                   "import time; print('before native stall'); time.sleep(30)"]
        with redirect_stdout(io.StringIO()) as output:
            with self.assertRaises(subprocess.TimeoutExpired):
                runner.run_process(command, os.environ.copy(), 1, inspect_output=True)
        self.assertIn("before native stall", output.getvalue())

    def test_fixture_cleanup_after_child_failure(self):
        for name in ("dialogs", "services"):
            with self.subTest(name=name):
                env = {}
                with self.assertRaises(subprocess.TimeoutExpired):
                    with runner.fixture(name, sys.executable, [], env):
                        if name == "dialogs":
                            path = Path(env["GQT_DIALOG_FIXTURE"])
                            self.assertEqual(path.name, "space café 日本.png")
                            self.assertTrue(path.read_bytes().startswith(b"\x89PNG"))
                        else:
                            path = Path(env["GLIMMER_QT_SERVICES_FIXTURE_DIR"])
                            self.assertTrue(path.is_dir())
                        raise subprocess.TimeoutExpired("fake-jolt", 60)
                self.assertFalse(path.exists())

    def test_layout_diagnostics_can_fail_a_successful_process(self):
        for warning in ("Binding loop detected", "Recursive rearrange"):
            with patch.object(runner, "run_process", return_value=(0, warning)):
                with self.assertRaisesRegex(RuntimeError, "layout loop"):
                    runner.run_suite("layout", "fake-jolt", [], {})

    def test_services_stays_offscreen_without_changing_other_suites(self):
        base_env = {"QT_QPA_PLATFORM": "cocoa"}
        with patch.object(runner, "run_process", return_value=(0, "")) as process:
            runner.run_suite("services", "fake-jolt", [], base_env)
            self.assertEqual(process.call_args.args[1]["QT_QPA_PLATFORM"], "offscreen")
            self.assertEqual(base_env, {"QT_QPA_PLATFORM": "cocoa"})


if __name__ == "__main__":
    unittest.main()
