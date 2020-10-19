import subprocess
import threading
import argparse
import multiprocessing

manager = multiprocessing.Manager()
final_list = manager.list()


CMD="java -cp jar/zk.jar -Dlog4j.configuration=log4j.properties " \
    "-DgigapaxosConfig=conf/exp/test.properties " \
    "-Djava.util.logging.config.file=conf/logging.properties " \
    "edu.umass.cs.BenchmarkClient "


class CommandThread (threading.Thread):
    def __init__(self, cmd):
        threading.Thread.__init__(self)
        self.cmd = cmd

    def run(self):
        p = subprocess.Popen(self.cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        output = p.communicate()
        final_list.append(output)


def main():
    parser = argparse.ArgumentParser(description='Run multiple XDN clients')
    # The command to execute
    parser.add_argument('-t', '--total', default=2, type=int,
                        help='total number of active replicas')
    parser.add_argument('-r', '--rate', default=1000, type=int,
                        help='sending rate of each client thread')
    parser.add_argument('-d', '--dest', default="172.17.0.2",
                        help='host (unused)')
    args = parser.parse_args()
    print args

    total = int(args.total)
    rate = int(args.rate)
    host = args.dest

    cmd = CMD + host+' '+str(rate)
    th_pool = []

    for i in range(total):
        th = CommandThread(cmd)
        th.start()
        th_pool.append(th)

    for th in th_pool:
        th.join()

    lats = []
    prob = []
    for l in final_list:
        lines = l[0].split('\n')
        # print lines
        lat = int(lines[-3])
        lats.append(lat)
        p = float(lines[-2])
        prob.append(p)
        
    print lats
    print prob
    print 'All done!'


if __name__ == "__main__":
    main()

