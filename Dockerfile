FROM clojure:openjdk-11-tools-deps as builder

WORKDIR /usr/src/app

# build time deps
RUN clj -Sdeps '{:deps {luchiniatwork/cambada {:mvn/version "1.0.5"}}}' -e :ok

COPY deps.edn /usr/src/app/deps.edn
# install deps
RUN clj -M -e :ok

COPY src/ /usr/src/app/src

RUN clj -M:uberjar

FROM ghcr.io/graalvm/native-image:ol7-java11-20.3.5-muslib as native

WORKDIR /usr/src/app

COPY --from=builder /usr/src/app/target/app-1.0.0-SNAPSHOT-standalone.jar /usr/src/app/app.jar

COPY reflect.json /usr/src/app/reflect.json

RUN ln -s $TOOLCHAIN_DIR/bin/x86_64-linux-musl-gcc $TOOLCHAIN_DIR/bin/musl-gcc

RUN native-image \
      --no-server \
      --no-fallback \
      --allow-incomplete-classpath \
      --initialize-at-build-time \
      --libc=musl \
      --static \
      --enable-url-protocols=https \
      -H:ReflectionConfigurationFiles=reflect.json \
      -H:+ReportExceptionStackTraces \
      --report-unsupported-elements-at-runtime \
      -jar app.jar packagetracker

FROM scratch

COPY --from=native /usr/src/app/packagetracker /packagetracker

CMD ["/packagetracker"]