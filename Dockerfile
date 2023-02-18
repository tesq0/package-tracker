FROM clojure:tools-deps-1.11.1.1224

RUN mkdir classes

COPY . .

RUN clojure -Spath
RUN clj -M -e "(compile 'package-tracker)"

EXPOSE 3000
CMD ["clojure", "-M", "-m", "package-tracker"]

