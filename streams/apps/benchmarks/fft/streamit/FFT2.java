/*
parsing:
done parsing. outputing:
 <# filter <# -> complex complex #> CombineDFT <# ( <# n int #> #> { <# Ws complex <# [ n #> #> <# init <# { <# wn complex <# = <# + <# $call <# . Math cos #> <# ( <# * 2 <# / <# . Math PI #> n #> #> #> #> <# * 1i <# $call <# . Math sin #> <# ( <# * 2 <# / <# . Math PI #> n #> #> #> #> #> #> #> #> <# = <# . <# $array Ws <# [ 0 #> #> real #> 1 #> <# = <# . <# $array Ws <# [ 0 #> #> imag #> 0 #> <# for <# i int <# = 1 #> #> <# < i n #> <# ++ i #> <# = <# $array Ws <# [ i #> #> <# * <# $array Ws <# [ <# - i 1 #> #> #> wn #> #> #> #> #> <# work <# push n #> <# peek n #> <# pop n #> <# { <# i int #> <# results complex <# [ n #> #> <# for <# = i 0 #> <# < i <# / n 2 #> #> <# ++ i #> <# { <# y0 complex #> <# y1 complex #> <# y1t complex #> <# = y0 <# peek i #> #> <# = y1 <# peek <# + <# / n 2 #> i #> #> #> <# = y1t <# * y1 <# $array Ws <# [ i #> #> #> #> <# = <# $array results <# [ i #> #> <# + y0 y1t #> #> <# = <# $array results <# [ <# + <# / n 2 #> i #> #> #> <# - y0 y1t #> #> #> #> <# for <# = i 0 #> <# < i n #> <# ++ i #> <# { pop <# push <# $array results <# [ i #> #> #> #> #> #> #> #> <# filter <# -> complex complex #> FFTReorderSimple <# ( <# n int #> #> { <# work <# peek n #> <# push n #> <# pop n #> <# { <# i int #> <# for <# = i 0 #> <# < i n #> <# += i 2 #> <# { <# push <# peek i #> #> #> #> <# for <# = i 1 #> <# < i n #> <# += i 2 #> <# { <# push <# peek i #> #> #> #> <# for <# = i 0 #> <# < i n #> <# ++ i #> <# { pop #> #> #> #> #> <# pipeline <# -> complex complex #> FFTReorder <# ( <# nWay int #> #> <# { <# n int <# = nWay #> #> <# while <# > n 2 #> <# { <# add FFTReorderSimple <# ( n #> #> <# = n <# / n 2 #> #> #> #> #> #> <# pipeline <# -> complex complex #> check <# ( <# nWay int #> #> <# { <# add FFTReorder <# ( nWay #> #> <# n int <# = 2 #> #> <# while <# <= n nWay #> <# { <# add CombineDFT <# ( n #> #> <# = n <# * n 2 #> #> #> #> #> #> <# filter <# -> void float #> source ( { <# i int #> <# init <# { <# = i 0 #> #> #> <# work <# pop 0 #> <# push 1 #> <# { <# push i #> <# ++ i #> #> #> #> <# filter <# -> float void #> sink ( { <# init { #> <# work <# pop 1 #> <# push 0 #> <# { pop #> #> #> <# pipeline <# -> void void #> FFT2 ( <# { <# add source ( #> <# add check <# ( 8 #> #> <# add sink ( #> #> #> null
done outputing. walking:
*/
import streamit.*;
import streamit.io.*;

class Complex extends Structure {
  public double real, imag;
}

