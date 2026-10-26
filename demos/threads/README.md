# Demo: threads de plataforma x virtual threads

`BenchmarkThreads.java` dispara 10.000 tarefas, e cada uma faz `Thread.sleep(1s)`:

1. com `Executors.newFixedThreadPool(200)`, o modelo clássico de pool de threads do SO;
2. com `Executors.newVirtualThreadPerTaskExecutor()`, as virtual threads do Java 21.

## Como rodar (JDK 21+)

Execute a partir desta pasta (`demos/threads`). Não é preciso rodar `javac` antes, porque o
Java 11+ executa um arquivo-fonte único diretamente:

```bash
java BenchmarkThreads.java
```

Resultado medido em um notebook comum:

```
Pool fixo (200 threads de plataforma)    10.000 tarefas em  50.346 ms
Virtual threads (uma por tarefa)         10.000 tarefas em   1.218 ms
```

> Dica para a apresentação: a primeira parte leva cerca de 50 s. Para encurtar, rode com
> menos tarefas, por exemplo `java BenchmarkThreads.java 2000` (cerca de 10 s contra 1 s).
> Para impressionar, rode só a parte virtual com 100.000 ou 1.000.000 de tarefas.

## Por que a diferença?

- No pool fixo, só 200 tarefas dormem ao mesmo tempo. As outras esperam na fila:
  10.000 / 200 = 50 "rodadas" de 1 s.
- Uma virtual thread que bloqueia (sleep, I/O, JDBC) é desmontada da thread do SO (a
  *carrier*), que fica livre para outra virtual thread. Assim as 10.000 dormem juntas.
- O código das tarefas é **idêntico** nos dois casos. Só o `ExecutorService` muda.

No PetClinic, o mesmo efeito é ligado com uma linha no `application.properties`:
`spring.threads.virtual.enabled=true`. A prova está em `http://localhost:8080/thread`.
