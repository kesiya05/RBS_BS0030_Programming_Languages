package main

import (
	"reflect"
	"testing"
)

// my own extra checks, the course tests are in main_test.go

func TestStreamSquares(t *testing.T) {
	var got []int
	for sq := range squaresStream([]int{1, 2, 3}) {
		got = append(got, sq)
	}
	if !reflect.DeepEqual(got, []int{1, 4, 9}) {
		t.Fatalf("stream got %v", got)
	}
	// empty input: the sender closes right away and range ends
	for range squaresStream([]int{}) {
		t.Fatal("empty stream should not send anything")
	}
}

func TestMutexSquares(t *testing.T) {
	got, sum := mutexSquares([]int{-3, 0, 2})
	if !reflect.DeepEqual(got, []int{9, 0, 4}) || sum != 13 {
		t.Fatalf("mutex got %v, %d", got, sum)
	}
}

func TestManyInputs(t *testing.T) {
	values := make([]int, 1000)
	for i := range values {
		values[i] = i
	}
	want, wantSum := sequentialSquares(values)
	got, sum := concurrentSquares(values)
	if !reflect.DeepEqual(got, want) || sum != wantSum {
		t.Fatalf("1000 inputs: results differ from sequential")
	}
}
