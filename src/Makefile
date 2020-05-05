PROJ=AnonGW

JAVA_FILES:=$(wildcard *.java)
JAVA_FILES_EX:=$(wildcard Exceptions/*.java)
#
# the rest is independent of the directory
#
JAVA_CLASSES:=$(patsubst %.java,%.class,$(JAVA_FILES))
JAVA_CLASSES_EX:=$(patsubst Exceptions/%.java,Exceptions/%.class,$(JAVA_FILES_EX))

.PHONY: classes
LIST:=

install: jar
	sudo chmod 0777 /usr/local/lib
	sudo chmod 0777 /usr/local/bin
	cp $(PROJ).jar /usr/local/lib
	echo "#!/bin/bash\njava -jar /usr/local/lib/$(PROJ).jar \"\$$@\"\n" > /usr/local/bin/$(PROJ)
	make clean
	sudo chmod +x /usr/local/bin/$(PROJ)

classes: $(JAVA_CLASSES) $(JAVA_CLASSES_EX)
	if [ ! -z "$(LIST)" ] ; then \
		javac -cp . $(LIST) ; \
	fi

$(JAVA_CLASSES) : %.class : %.java
	$(eval LIST+=$$<)

$(JAVA_CLASSES_EX) : Exceptions/%.class : Exceptions/%.java
	$(eval LIST+=$$<)

clean:
	find . -type f -iname \*.class -delete
	rm -rf $(PROJ).jar

jar: classes
	jar cfe $(PROJ).jar $(PROJ) *.class Exceptions/*.class
