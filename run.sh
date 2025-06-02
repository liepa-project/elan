#!/bin/bash
#java -Xmx256m -Dswing.aatext=true -DPreferredMediaFramework=VLCJ --add-opens java.base/java.lang=ALL-UNNAMED -Djava.library.path=${java.library.path};${project.build.directory}/${nativelib.dir} -classpath mpi.eudico.client.annotator.ELAN

cd target
CLASSPATH=$(find ./lib -name "*.jar" -print0 | xargs -0 echo -n | tr ' ' ':')

#jdeps  --class-path $CLASSPATH:./elan-6.9.jar list

java -Xmx256m -Dswing.aatext=true -DPreferredMediaFramework=VLCJ --add-opens java.base/java.lang=ALL-UNNAMED   -classpath $CLASSPATH:./elan-6.9.jar  mpi.eudico.client.annotator.ELAN
