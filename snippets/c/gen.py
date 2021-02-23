
def gen_many_loops():
    f = open("many_while.c", "w")
    f.write("int main() {\n")

    f.write("\tint a0 = 1;\n")
    for i in range(1, 3000):
        f.write(f"\tint a{i};\n")
        f.write(f"\twhile(a{i-1} > 0) {{\n")
        f.write(f"\t\ta{i} = a{i - 1};\n")
        f.write(f"\t\ta{i-1}--;\n")
        f.write("\t}\n")

    f.write("\treturn a99;\n")
    f.write("}\n")


    f = open("many_while.fls", "w")

    f.write("fn main() {\n")

    n = 300
    f.write("\ta0 := 1;\n")
    for i in range(1, n):
        f.write(f"\ta{i} := 0;\n")
        f.write(f"\twhile(a{i-1} > 0) {{\n")
        f.write(f"\t\ta{i} := a{i - 1};\n")
        f.write(f"\t\ta{i-1} := a{i-1} - 1;\n")
        f.write("\t};\n")

    f.write(f"\treturn a{n-1};\n")
    f.write("}\n")
def gen_many_vars(n):
    f = open("many_vars.c", "w")
    f.write("int main() {\n")

    f.write("\tint a0 = 1;\n")
    for i in range(1, n):
        f.write(f"\tint a{i} = a{i - 1} + 1;\n")

    f.write(f"\treturn a{n - 1};\n")
    f.write("}\n")    

    f = open("many_vars.fls", "w")
    f.write("fn main() {\n")
    f.write("\ta0 := 1;\n")
    for i in range(1, n):
        f.write(f"\ta{i} := a{i-1} + 1;\n")
    f.write(f"\tprint(a{n-1});\n")
    f.write(f"\treturn a{n-1};\n")
    f.write("}\n")
def gen_many_vars2(n):
    f = open("many_vars2.c", "w")
    f.write("int main() {\n")

    f.write("\tint a0 = 1;\n")
    f.write("\tint a1 = 1;\n")
    for i in range(2, n, 2):
        f.write(f"\tint a{i} = a{i - 1} + 1;\n")
        f.write(f"\tint a{i+1} = a{i - 2} + 1;\n")

    f.write(f"\tint a{n} = a{n-1} + a{n-2};\n")

    f.write(f"\treturn a{n};\n")
    f.write("}\n")    

    f = open("many_vars2.fls", "w")
    f.write("fn main() {\n")
    f.write("\ta0 := 1;\n")
    f.write("\ta1 := 1;\n")
    for i in range(2, n, 2):
        f.write(f"\ta{i} := a{i - 1} + 1;\n")
        f.write(f"\ta{i+1} := a{i - 2} + 1;\n")
     
    f.write(f"\ta{n} := a{n-1} + a{n-2};\n")
   
    f.write(f"\tprint(a{n});\n")
    f.write(f"\treturn a{n};\n")
    f.write("}\n")
def gen_many_branch(n):
    f = open("many_branch.c", "w")
    f.write("int main() {\n")

    f.write("\tint a0 = 1;\n")
    for i in range(1, n):
        f.write(f"\tint a{i};\n")
        f.write(f"\tif(a{i-1} > 0) a{i} = a{i-1};\n")

    f.write(f"\treturn a{n-1};\n")
    f.write("}\n")    

    f = open("many_branch.fls", "w")
    f.write("fn main() {\n")
    f.write("\ta0 := 1;\n")
    for i in range(1, n):
        f.write(f"\tif(a{i-1} > 0) {{ a{i} := a{i-1}; }} else {{}}\n")
     
    f.write(f"\ta{n} := a{n-1};\n")
   
    f.write(f"\tprint(a{n});\n")
    f.write(f"\treturn a{n};\n")
    f.write("}\n")

def gen_large_expr(n):
    f = open("large_expr.fls", "w")
    f.write("fn main() {\n")
    f.write("\ta := 1")
    for i in range(1, n):
        f.write(f" + 1")
    f.write(";\n")
     
   
    f.write(f"\tprint(a);\n")
    f.write(f"\treturn a;\n")
    f.write("}\n")

gen_many_loops()
gen_many_vars(100)
gen_many_vars2(100)
gen_many_branch(100)

gen_large_expr(40)
