// scratch program for the deadlock exercise, not part of the assessed code.
// run from labs/week11/work: go run scratch/deadlock.go
//
// broken version (don't run it as a real program):
//
//	ch := make(chan int)
//	ch <- 1              // blocks forever: unbuffered send waits for a receiver,
//	fmt.Println(<-ch)    // but the receive is on the next line of the same goroutine
//
// go stops it with "fatal error: all goroutines are asleep - deadlock!"
package main

import "fmt"

func main() {
	// fix 1: send from another goroutine, so main can get to the receive
	ch := make(chan int)
	go func() { ch <- 1 }()
	fmt.Println("fix 1 (sending goroutine):", <-ch)

	// fix 2: buffer of 1, the send stores the value and returns right away
	buf := make(chan int, 1)
	buf <- 1
	fmt.Println("fix 2 (capacity-one buffer):", <-buf)
}
