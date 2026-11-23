// Week 11 lab - squares with goroutines and a channel (start of A6)
//
// run from labs/week11/work:
// go run main.go
// GO111MODULE=off go test -v
// GO111MODULE=off go test -race -count=10
// go run -race main.go
package main

import (
	"fmt"
	"sync"
)

type Result struct {
	Index int
	Value int
}

// baseline from the starter, kept to compare with
func sequentialSquares(values []int) ([]int, int) {
	ordered := make([]int, len(values))
	total := 0
	for i, n := range values {
		ordered[i] = n * n
		total += ordered[i]
	}
	return ordered, total
}

// one goroutine per input. workers only send a Result, they never touch
// ordered or total. the collector (this function) is the only owner of both.
func concurrentSquares(values []int) ([]int, int) {
	ordered := make([]int, len(values))
	total := 0

	// unbuffered: each send waits until the collector receives it
	results := make(chan Result)

	for i, n := range values {
		// pass i and n as arguments so every worker gets its own copy
		go func(index, value int) {
			results <- Result{Index: index, Value: value * value}
		}(i, n)
	}

	// results can arrive in any order, the scheduler decides which worker
	// runs first. using the index puts each one back in its place.
	// we know exactly how many workers there are, so we receive exactly that
	// many. for empty input the loop runs 0 times and nothing waits.
	for range values {
		r := <-results
		ordered[r.Index] = r.Value
		total += r.Value
	}

	return ordered, total
}

// single-sender stream: here only one goroutine sends, so it is the one that
// closes the channel when it's done. the receiver can then use range and stops
// at close. in concurrentSquares there are many senders, so none of them can
// close (one closing early would make the others panic) - we count instead.
func squaresStream(values []int) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out)
		for _, n := range values {
			out <- n * n
		}
	}()
	return out
}

// extension: same result but the total is shared and protected by a mutex
// instead of owned by one collector.
func mutexSquares(values []int) ([]int, int) {
	ordered := make([]int, len(values))
	total := 0
	var mu sync.Mutex
	var wg sync.WaitGroup

	for i, n := range values {
		wg.Add(1)
		go func(index, value int) {
			defer wg.Done()
			sq := value * value
			mu.Lock()
			ordered[index] = sq
			total += sq
			mu.Unlock()
		}(i, n)
	}
	wg.Wait()
	return ordered, total
}

func main() {
	values := []int{1, 2, 3, 4}

	ordered, total := concurrentSquares(values)
	fmt.Println(ordered, total) // [1 4 9 16] 30

	seqOrdered, seqTotal := sequentialSquares(values)
	fmt.Println("sequential:", seqOrdered, seqTotal)

	emptyOrdered, emptyTotal := concurrentSquares([]int{})
	fmt.Println("empty:", emptyOrdered, emptyTotal)

	fmt.Print("stream:")
	for sq := range squaresStream(values) {
		fmt.Print(" ", sq)
	}
	fmt.Println()

	mOrdered, mTotal := mutexSquares(values)
	fmt.Println("mutex version:", mOrdered, mTotal)
}
