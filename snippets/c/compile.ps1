param ([Parameter(Mandatory)]$name)
nasm -fwin64 "$name.asm"
gcc -o "$name.exe" "$name.obj"
Measure-Command { clang -S -emit-llvm -O2 "$name.c" | Out-Default }
