.PHONY: clean compile test jar uberjar repl

all: clean jar install kfxc

clean:
	clj -T:build clean

jar: clean
	clj -T:build jar

install: jar
	clj -T:build install

kfxc:
	clj -T:build kfxc