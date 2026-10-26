#!/usr/bin/env bash
# Dépendances cloud, hors du checkout ; téléchargements TLS et empreintes officielles.
set -euo pipefail
mkdir -p /workspace/tools /workspace/.m2/repository
if [ ! -x /workspace/tools/apache-maven-3.9.11/bin/mvn ]; then
  curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz -o /tmp/arena-maven.tar.gz
  curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz.sha512 -o /tmp/arena-maven.sha512
  python - <<'PY'
import hashlib, pathlib
assert hashlib.sha512(pathlib.Path('/tmp/arena-maven.tar.gz').read_bytes()).hexdigest() == pathlib.Path('/tmp/arena-maven.sha512').read_text().split()[0]
PY
  tar -xzf /tmp/arena-maven.tar.gz -C /workspace/tools
fi
if [ ! -x '/workspace/tools/jdk-21.0.9+10/bin/javac' ]; then
  jdk_url='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.9%2B10/OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10.tar.gz'
  curl -fsSL "$jdk_url" -o /tmp/arena-jdk.tar.gz
  curl -fsSL "$jdk_url.sha256.txt" -o /tmp/arena-jdk.sha256
  python - <<'PY'
import hashlib, pathlib
assert hashlib.sha256(pathlib.Path('/tmp/arena-jdk.tar.gz').read_bytes()).hexdigest() == pathlib.Path('/tmp/arena-jdk.sha256').read_text().split()[0]
PY
  tar -xzf /tmp/arena-jdk.tar.gz -C /workspace/tools
fi
python - <<'PY'
import os, urllib.parse, xml.etree.ElementTree as E
settings = E.Element('settings'); proxies = E.SubElement(settings, 'proxies')
for scheme in ('http', 'https'):
    url = urllib.parse.urlparse(os.environ.get(scheme.upper() + '_PROXY', ''))
    if url.hostname:
        proxy = E.SubElement(proxies, 'proxy')
        for key, value in {'id': scheme, 'active': 'true', 'protocol': scheme, 'host': url.hostname,
                           'port': str(url.port or 80), 'nonProxyHosts': 'localhost|127.0.0.1'}.items():
            E.SubElement(proxy, key).text = value
# Aucune valeur de credential n'est copiée.
E.ElementTree(settings).write('/workspace/tools/maven-settings.xml')
PY
cd /workspace/POO
./mvn-cloud -B verify
