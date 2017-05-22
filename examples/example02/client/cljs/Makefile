SRCS = $(shell find src -type f)
LEIN = $(shell which lein || echo ../../../../lein)

all: client

client: ./target/nodecljs/main.js

./target/nodecljs/main.js: $(SRCS) Makefile
	$(LEIN) nodecompile
	@echo "Compilation complete: use \"node $@ --help\" for execution instructions"

clean:
	-@rm -rf ./target ||:
	-@rm -rf ./node_modules ||:
