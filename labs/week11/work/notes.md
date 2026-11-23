# Week 11 notes

Files in labs/week11/work:
- main.go - my code (concurrentSquares, plus a stream example and a mutex version)
- main_test.go - the course tests, copied without changes
- extra_test.go - my own extra tests (stream, mutex version, 1000 inputs)
- scratch/deadlock.go - the deadlock exercise, separate from the assessed code
- output.txt - what all the commands printed

Commands, from labs/week11/work:

    go run main.go
    GO111MODULE=off go test -v
    GO111MODULE=off go test -race -count=10
    go run -race main.go
    go run scratch/deadlock.go

All tests pass, the race detector found nothing in 10 runs, and main prints [1 4 9 16] 30.

## How concurrentSquares works

For every input I start one goroutine and pass it the index and the value as arguments. Each worker squares its number and sends Result{Index, Value} on an unbuffered channel. That's all a worker does.

The collector is the loop at the bottom of concurrentSquares. It receives the results and is the only code that writes to ordered and total.

## Ownership

ordered and total belong only to the collector. Workers never write to them, they only send messages. Since only one goroutine touches that data, there's nothing to race on and I didn't need a lock. This is the "share memory by communicating" idea: the data moves through the channel instead of being shared.

## Ordering

Results arrive in whatever order the scheduler runs the workers, and that can change from run to run. A worker that started later can finish first. So I can't just append results as they arrive. I use r.Index to put each value back in its original position, and the output is always in input order.

The total doesn't depend on order because addition gives the same sum either way.

## Completion

I start exactly len(values) workers and each sends exactly one Result, so the collector receives exactly len(values) times and then stops. For empty input no workers start, the loop runs 0 times, and it returns an empty slice and 0 without waiting on anything.

I don't close the results channel because there are many senders. If one worker closed it, the others would panic when sending on a closed channel, and no single worker knows when the others are done. So the collector counts instead.

squaresStream is the other case: only one goroutine sends, so that same goroutine closes the channel when it's finished (defer close(out)). The receiver can then use `for sq := range ...`, which ends when the channel is closed.

## Race vs deadlock

A race is when two goroutines touch the same data at the same time and at least one writes. The result then depends on timing, for example if every worker did total += sq without a lock.

A deadlock is when goroutines wait on each other forever and nothing can move. The scratch example:

    ch := make(chan int)
    ch <- 1
    fmt.Println(<-ch)

The send on an unbuffered channel waits for a receiver. The receive is on the next line in the same goroutine, so it's never reached. Go stops with "fatal error: all goroutines are asleep - deadlock!". scratch/deadlock.go has two fixes: send from a separate goroutine, or use make(chan int, 1) so the send can store one value and continue. I left the broken version only as a comment so the assessed program always runs.

## Why passing tests don't prove it's correct

The tests and the race detector only check the schedules that actually happened during those runs. A different machine or load could run the goroutines in another order. The race detector also only reports races on code paths that ran. So a clean run is good evidence but not proof. The real reason it's safe is the design: only the collector writes the shared data, and it receives exactly one message per worker.

Also, the starter's sequential placeholder already passed the tests, so passing tests alone didn't show the TODO was done. TestManyInputs with 1000 goroutines compares against the sequential version to check it with more interleaving.

## Concurrency vs parallelism

Concurrency means the program is structured as independent tasks that can make progress in overlapping time. Parallelism means they actually run at the same moment on different cores. My workers are concurrent. Whether they run in parallel depends on the machine and the scheduler, and the code is correct either way.

## Extension: collector vs mutex

mutexSquares gets the same result differently. Every worker writes into ordered and total itself, protected by a sync.Mutex, and a WaitGroup waits until all workers are done.

- In the collector version, only one goroutine owns the data, so no lock is needed. Completion is counted by received messages.
- In the mutex version the data is shared, so every write must hold the lock. If I forgot mu.Lock() on one line, it would be a race (go test -race would likely catch it). Completion is tracked with wg.Add and wg.Done.

For this small task both work. I find the collector version easier to reason about because there's one place that changes the data. No speedup was measured, and squaring numbers is too cheap for goroutines to be faster than the loop anyway.
