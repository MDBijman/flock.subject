# flocksubject Language Specification

Compiling the resulting .asm file (Windows):

Requires `nasm`, `gcc`, `llvm`

`nasm -fwin64 <file.asm>`

`gcc -o test.exe test.obj`

`clang -S -emit-llvm -O2 test.c`

set  disassemble-next-line on
show disassemble-next-line

