
FILES := $(shell find src -type f)
DEST := /var/lib/tomcat8/webapps/ireader.war

THIS_FILE := $(lastword $(MAKEFILE_LIST))

CONT_TOUCH := .cont-touch

ifeq ($(CMD),)
first : deploy
else
first : cont
endif

deploy : $(DEST)
$(DEST) : target/ireader.war
	cp $< $@

target/ireader.war : pom.xml $(FILES)
	nice mvn package

clean :
	mvn clean
	find -name '*.js' -delete
	rm -f $(CONT_TOUCH)

distclean : clean
	rm $(DEST)

cont :
	while sleep 1; do nice $(MAKE) -s -f $(THIS_FILE) $(CONT_TOUCH); done

$(CONT_TOUCH) : $(TCH)
	touch $@
	-$(CMD)

.PHONY : deploy clean distclean cont first
