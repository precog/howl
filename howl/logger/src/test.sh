java_opts="-Xint -cp ./HOWL.zip \
  -Dxa.msg.count=50 \
  -Dxa.msg.size=80 \
  -Dxa.workers=400 \
  -Dhowl.log.maxWaitingThreads=200 \
  -Dhowl.log.flushSleepTime=50 \
  -Dhowl.log.bufferSize=8 \
  -Dhowl.log.bufferPoolSize=2 \
  -Dhowl.log.maxBlocksPerFile=200 \
  -Dhowl.log.LogFile.maxLogFiles=3 \
  -Dhowl.log.LogFile.dir=./logs \
  -Dhowl.log.LogFile.filename=xa \
  -Dhowl.log.LogFile.ext=log \
  -Dhowl.log.test.setautomark=true \
  -Dhowl.LogBuffer.class=org.objectweb.howl.log.BlockLogBuffer \
  -Dhowl.LogBuffer.checksum=true \
"

class=org.objectweb.howl.test.LogTest
time java $java_opts $class

