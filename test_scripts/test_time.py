import os
import sys

import subprocess

# avg_times = 3
# splitRange = [8,16,32,64,128,256]
# sizeRange = [2,4,8,16,32]
# with open("time.csv",'w') as timeFile:
#     timeFile.write("filesize, splitnum, time1, time2, time3, averagetime\n")
#     for i in sizeRange:
#         for j in splitRange:
#             sentence = ((str(i) + "G, ") + str(j) + ", ")
#             total_time = 0
#             for k in range(avg_times):
#                 cmd = """rm -rf ~/diankun/Output/* && java -cp target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar xgboostsparksgx.SplitAndEncryptForXgboost ~/diankun/data/""" + str(i) + """G_data LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY= ~/diankun/Output """ + str(j)
#                 ret, val = subprocess.getstatusoutput(cmd)
#                 for line in val.split("\n"):
#                     if "time" in line:
#                         tmpTime = line.split(" ")[3]
#                         sentence += (tmpTime+", ")
#                         total_time += int(tmpTime)
#                         break
#             avg_time = int(total_time/avg_times)
#             sentence += str(avg_time)
#             print("SUCCESS SUCCESS " + sentence)
#             timeFile.write(sentence + "\n")

avg_times = 3
splitRange8 = [32,64,128,256]
splitRange16 = [64,128,256,512]
splitRange32 = [128,256,512]
sizeRange = [8,16,32]
with open("time.csv",'w') as timeFile:
    timeFile.write("filesize, splitnum, time1, time2, time3, averagetime\n")
    for i in sizeRange:
        if i == 8:
            for j in splitRange8:
                sentence = ((str(i) + "G, ") + str(j) + ", ")
                total_time = 0
                for k in range(avg_times):
                    cmd = """rm -rf ~/diankun/Output/* && java -cp target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar xgboostsparksgx.SplitAndEncryptForXgboost ~/diankun/data/""" + str(i) + """G_data LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY= ~/diankun/Output """ + str(j)
                    ret, val = subprocess.getstatusoutput(cmd)
                    for line in val.split("\n"):
                        if "time" in line:
                            tmpTime = line.split(" ")[3]
                            sentence += (tmpTime+", ")
                            total_time += int(tmpTime)
                            break
                avg_time = int(total_time/avg_times)
                sentence += str(avg_time)
                print("SUCCESS SUCCESS " + sentence)
                timeFile.write(sentence + "\n")
        elif i == 16:
            for j in splitRange16:
                sentence = ((str(i) + "G, ") + str(j) + ", ")
                total_time = 0
                for k in range(avg_times):
                    cmd = """rm -rf ~/diankun/Output/* && java -cp target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar xgboostsparksgx.SplitAndEncryptForXgboost ~/diankun/data/""" + str(i) + """G_data LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY= ~/diankun/Output """ + str(j)
                    ret, val = subprocess.getstatusoutput(cmd)
                    for line in val.split("\n"):
                        if "time" in line:
                            tmpTime = line.split(" ")[3]
                            sentence += (tmpTime+", ")
                            total_time += int(tmpTime)
                            break
                avg_time = int(total_time/avg_times)
                sentence += str(avg_time)
                print("SUCCESS SUCCESS " + sentence)
                timeFile.write(sentence + "\n")
        else:
            for j in splitRange32:
                sentence = ((str(i) + "G, ") + str(j) + ", ")
                total_time = 0
                for k in range(avg_times):
                    cmd = """rm -rf ~/diankun/Output/* && java -cp target/xgboostsparksgx-1.0-SNAPSHOT-jar-with-dependencies.jar xgboostsparksgx.SplitAndEncryptForXgboost ~/diankun/data/""" + str(i) + """G_data LDlxjm0y3HdGFniIGviJnMJbmFI+lt3dfIVyPJm1YSY= ~/diankun/Output """ + str(j)
                    ret, val = subprocess.getstatusoutput(cmd)
                    for line in val.split("\n"):
                        if "time" in line:
                            tmpTime = line.split(" ")[3]
                            sentence += (tmpTime+", ")
                            total_time += int(tmpTime)
                            break
                avg_time = int(total_time/avg_times)
                sentence += str(avg_time)
                print("SUCCESS SUCCESS " + sentence)
                timeFile.write(sentence + "\n")
