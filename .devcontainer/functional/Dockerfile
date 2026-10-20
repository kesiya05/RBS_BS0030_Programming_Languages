# BS0030 functional laboratory: Clojure CLI on Java 25.
# The CLI version is pinned by the image tag; the Clojure language version
# used by Week 3 is pinned separately in labs/week03/deps.edn.
FROM clojure:temurin-25-tools-deps-1.12.5.1664-trixie-slim

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        git openssh-client curl ca-certificates sudo bash rlwrap \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 1000 vscode \
    && useradd --uid 1000 --gid 1000 --create-home --shell /bin/bash vscode \
    && echo 'vscode ALL=(root) NOPASSWD:ALL' > /etc/sudoers.d/vscode \
    && chmod 0440 /etc/sudoers.d/vscode \
    && mkdir -p /home/vscode/.clojure /home/vscode/.m2 \
    && chown -R vscode:vscode /home/vscode/.clojure /home/vscode/.m2

ENV JAVA_HOME=/opt/java/openjdk
