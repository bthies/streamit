all: demo

# demo the problems straight from CPP and Java files
demo:
	gcc34 -lm round_demo.c -o round_demo
	jikes -source 1.4 RoundDemo.java
	./test_round && java RoundDemo
