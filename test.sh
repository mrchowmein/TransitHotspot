
#!/bin/bash
#
#
#remove old files from hdfs
hdfs dfs -rm -r /2019060601OutputTestLog
hdfs dfs -rm -r /testRef

#create refcount file from sample
python3 ./Testing/createRefCount.py
hdfs dfs -mkdir /testRef
#move refcount file to hdfs
hdfs dfs -put ./Testing/RefSource/2019060601RefCount.csv /testRef

#run output test
spark-shell -i ./Testing/testOutput.scala --conf spark.driver.args="$1 $2" --jars /home/ubuntu/postgresql-42.2.8.jar
