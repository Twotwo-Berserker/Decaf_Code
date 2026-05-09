

# Java Compilation and Execution Guide
This guide will help you compile and run your Java program from the command line.

## Compilation

First, navigate to the root of your project directory. Use the javac command to compile all .java files in your project. The -d option specifies the destination directory for the compiled .class files. We'll compile them into a bin directory.

```bash
javac -d bin $(find . -name "*.java")
```
## Execution
run the following command to execute the program. The -cp option specifies the classpath, and the main class is main.Main.
```bash
(base) ➜ java -cp bin main.Main 
int a; # this line is your input, press enter to end input

(ID , int)
(ID , a)
(SYM , ;)

exit # this line is your input, press enter to end input
(ID , exit)
(base) ➜  
```