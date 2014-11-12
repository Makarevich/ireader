
# THIS_FILE := $(lastword $(MAKEFILE_LIST))

run :
	sbt ~container:start

package :
	sbt package

clean :
	sbt clean
	rm -r target project/target project/project

.PHONY : run package clean
