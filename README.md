# MProfiler

MProfiler is a simple Java profiler class that requires
updating your code to target which methods you want to
profile.

Sample usage:

1. MProfiler.reset(); // on repeated runs, we need to clear the list first
1. MProfiler.start("label1"); // start a new profile point
1. MProfiler.start("label2", true); // start another, while others are still running; the 'true' argument means 'print when this starts and stops'
1. MProfiler.stop(); // stop the last started profile point
1. MProfiler.stop(true); // when stopping the final profile point, pass true to get a summary printed

The Java class `App` (in this repo) demonstrates using
MProfiler.  Running the class will generate output similar
to the following:

```
% ./gradlew run
Calculating task graph as no cached configuration is available for tasks: run

> Task :app:run
% Time    Total Time (ms)    # Calls    MS / Call    Label
90.74%    5,378              100        53.78        Sleeping-2
9.26%     5,927              10         54.9         Sleeping-1
0%        5,927              1          0            doStuff
```

