/* file "time_exec.c" */

/*  Copyright (c) 1994 Stanford University

    All rights reserved.

    This software is provided under the terms described in
    the "suif_copyright.h" include file. */

//#include <suif_copyright.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include <sys/types.h>
#include <sys/param.h>
#include <sys/signal.h>
#include <sys/wait.h>
#include <signal.h>
#include <unistd.h>

#define TIMEOUT         -1


pid_t pid1, pid2, ppid;
int timeout;

static void alfun()
{
    fprintf(stderr, "Error: Timeout after %d seconds...\n", timeout);
}

static void dumbfun()
{

}

static void usage_exit(char *pname)
{
    printf("Usage: %s [-v] [-t <time_out>] program [arg list....]\n", pname);
    printf("\t\t-t <timeout>  "
	   "Timeout value in seconds(default is infinity)\n");
    printf("\t\t-s <timeout>  Timeout value in seconds\n");
    printf("\t\t-m <timeout>  Timeout value in minutes\n");
    printf("\t\t-h <timeout>  Timeout value in hours\n");
    printf("\t\t-v            Print execution time\n");
    exit(-5);
}


int
main(int argc, char **argv, char **envp)
{
    char **ag;
    int tmp, stprog;
    pid_t rpro;
    int prt_time;
    int wt;
    time_t bef;
    time_t aft;
    double diff;

    if(argc < 2) {
        usage_exit(argv[0]);
    }

    timeout = TIMEOUT;
    prt_time = 0;

    for(tmp=1; tmp < argc; tmp++){
        if(argv[tmp][0] != '-') {
            ag = &argv[tmp];
            stprog = tmp;
            tmp = argc+1;
            }
        else {
            switch(argv[tmp][1]) {
            case 't' :
            case 'T' :
            case 's' :
            case 'S' :
                timeout = atoi(argv[++tmp]);
                break;

            case 'm' :
            case 'M' :
                timeout = 60*atoi(argv[++tmp]);
                break;

            case 'h' :
            case 'H' :
                timeout = 60*60*atoi(argv[++tmp]);
                break;

            case 'v' :
            case 'V' :
                prt_time = 1;
                break;
                
            default:
                printf("Unknown flag '%c'\n", argv[tmp][1]);
                usage_exit(argv[0]);
            }
        }
    }


    ppid = getpid();

    fflush(stdout);


    /* Structure of processes here:
     * -- The main process forks a child; its process ID is pid1.
     *    That process sets an alarm() and goes to sleep.  It wakes
     *    up from either SIGALRM or SIGTERM (the alarm goes off or
     *    it is killed), and in either case exits.
     * -- The main process forks a second child; its process ID is
     *    pid2.  That just execs the child process.
     * -- The main process wait()s for a child to exit.  If the
     *    pid of the exiting child is pid1, it's because there
     *    was a timeout; if the pid is pid2, it's because the
     *    desired program exited.
     * We make ourself a process group leader.  Then when we want
     * to clean up the children, we can kill pid 0 to kill everything
     * in the current process group; since we ignore SIGTERM ourselves,
     * this has the desired effect.
     */

    setpgid(0, 0);
    switch(pid1 = fork()) {
    case -1 :
        printf("Cannot create a new process\n");
        exit(-3);

    default:

        switch(pid2 = fork()) { 
        case -1 : 
            printf("Cannot create a newprocess\n"); 
            exit(-3);

        case 0 :
            signal(SIGTERM, dumbfun);
            execvp(argv[stprog], ag);
            printf("Cannot execute %s\n", argv[stprog]);
            exit(-2);
            break;

        default:
            signal(SIGTERM, dumbfun);

            bef = time(NULL);


            if((rpro = wait(&wt)) == -1) {
                printf("Wait error\n");
                kill(pid1, SIGTERM); 
                kill(pid2, SIGTERM); 
                exit(-4);
            }

            aft = time(NULL);
            diff = difftime(aft, bef);


            if(rpro == pid2) {
                kill(pid1, SIGTERM); 

                if(prt_time) {
                    double whole_seconds;
                    double whole_minutes;
                    double whole_hours;
                    int hundreth_remainder;
                    int second_remainder;
                    int minute_remainder;

                    whole_seconds = floor(diff);
                    hundreth_remainder =
                            (int)(floor((diff - whole_seconds) * 100 + 0.5));
                    whole_minutes = floor(whole_seconds / 60);
                    second_remainder =
                            (int)(whole_seconds - (whole_minutes * 60));
                    whole_hours = floor(whole_minutes / 60);
                    minute_remainder =
                            (int)(whole_minutes - (whole_hours * 60));

                    fprintf(stderr, "%02.0f:%02d:%02d.%02d \n", whole_hours,
                            minute_remainder, second_remainder,
                            hundreth_remainder);
                }

		if (WIFEXITED(wt))
                    exit(WEXITSTATUS(wt));
                else
                    exit(-1);

            } else {
                kill(0, SIGTERM); 
                exit(-1);
            }



    }

    case 0:
        signal(SIGALRM, alfun);
        signal(SIGTERM, dumbfun);
        if(timeout>0)
            alarm(timeout);
        pause();
        alarm(0);
    }

    return 0;
}


