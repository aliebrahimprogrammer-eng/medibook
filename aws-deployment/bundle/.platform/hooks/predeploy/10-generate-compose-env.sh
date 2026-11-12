#!/bin/bash
set -euo pipefail

python3 - <<'PY'
import json
import os
import subprocess
from pathlib import Path

result = subprocess.check_output(
    ["/opt/elasticbeanstalk/bin/get-config", "environment"],
    text=True
)
properties = json.loads(result)

keys = [
    "DB_URL",
    "DB_USERNAME",
    "DB_PASSWORD",
    "JWT_SECRET",
    "ADMIN_EMAIL",
    "ADMIN_PASSWORD",
    "USER_SERVICE_INTERNAL_API_KEY",
    "USER_SERVICE_PUBLIC_URL",
    "KAFKA_BOOTSTRAP_SERVERS",
    "MAIL_HOST",
    "MAIL_PORT",
    "MAIL_USERNAME",
    "MAIL_PASSWORD",
]

missing = [key for key in keys if key not in properties]
if missing:
    raise SystemExit("Missing required Elastic Beanstalk properties: " + ", ".join(missing))

def quote_env(value):
    value = str(value).replace("\\", "\\\\").replace("'", "\\'")
    return "'" + value + "'"

content = "".join(
    key + "=" + quote_env(properties[key]) + "\n"
    for key in keys
)

env_file = Path.cwd() / ".env"
env_file.write_text(content, encoding="utf-8")
os.chmod(env_file, 0o600)
PY