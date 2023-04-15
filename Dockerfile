FROM clojure:temurin-8-tools-deps-bullseye-slim

WORKDIR /usr/src/app

COPY deps.edn /usr/src/app/deps.edn

# install deps
RUN clj -M -e :ok

COPY src/ /usr/src/app/src

CMD ["clj", "-M", "-m", "packagetracker.main"]
