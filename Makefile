NAME=chaintool
LEIN = $(shell which lein || echo ./lein)
BINDIR ?= /usr/local/bin
OUTPUT=target/$(NAME)
META=org.hyperledger.chaintool.meta

SRCS += $(shell find src -type f)
SRCS += $(shell find resources -type f)

PROTOS += $(shell find resources/proto -name "*.proto")
PROTOS += target/$(META).proto

all: $(OUTPUT)

$(OUTPUT): $(SRCS) Makefile project.clj
	@$(LEIN) bin

$(PREFIX)$(BINDIR):
	mkdir -p $@

# Bootstrap!  We use chaintool to build a .proto for chaintool.
# If HEAD will not build, an older release of chaintool may be
# used to generate this file by hand
target/$(META).proto: resources/metadata/$(META).cci $(OUTPUT)
	$(OUTPUT) proto -o $@ $<

proto: $(PROTOS)
	protoc --java_out=./src $(PROTOS)

install: $(OUTPUT) $(PREFIX)$(BINDIR)
	cp $(OUTPUT) $(PREFIX)$(BINDIR)

clean:
	@echo "Cleaning up.."
	@$(LEIN) clean
	-@rm -rf target
	-@rm -f *~
