"""Shared build and runtime setup for the repository's launchers."""

import os
from pathlib import Path
import shutil
import subprocess
import sys


ROOT = Path(__file__).resolve().parent.parent


def executable(name):
    path = shutil.which(name)
    if path is None:
        raise RuntimeError(f"Required executable not found: {name}")
    # Resolve relative overrides before launching anything from ROOT. Keep the
    # executable's symlink name, which some launchers use to find their runtime.
    return os.path.abspath(path)


def jolt():
    return executable(os.environ.get("JOLT") or "jolt")


def build_environment(test_fixtures=False):
    command = [str(ROOT / "build-native")]
    if test_fixtures:
        command.append("CONFIG+=property_tests")
    subprocess.run(command, cwd=ROOT, check=True)
    qmake = (ROOT / "build/qmake-path").read_text().strip()
    env = os.environ.copy()
    for variable, query in (("QT_PLUGIN_PATH", "QT_INSTALL_PLUGINS"),
                            ("QML_IMPORT_PATH", "QT_INSTALL_QML")):
        env[variable] = subprocess.check_output([qmake, "-query", query], text=True).strip()
    return env


def cli(main):
    try:
        return main()
    except (OSError, RuntimeError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1
    except subprocess.CalledProcessError as error:
        return error.returncode if error.returncode > 0 else 1
    except KeyboardInterrupt:
        return 130
