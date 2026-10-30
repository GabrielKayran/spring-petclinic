# Roteiro da aula: Estudo de caso (Grupo 07, POO2)

Material de apoio para o grupo. Detalhes técnicos de cada extensão estão no [ESTUDO-DE-CASO.md](ESTUDO-DE-CASO.md),
e os diagramas, em [docs/uml](docs/uml/README.md).

- **Site:** `https://petclinic-poo2.onrender.com/painel` (menu "Estudo de caso")
- **Slides da teoria (29 slides, com notas do apresentador):** https://claude.ai/artifact/E6QxzjL5bopc1RdBjEyAuc
- **Formato:** 70 min de teoria (terça) + 70 min de prática (quinta), com tolerância de ±10 min
- **Nota de cada dia (35 pts):** 10 tempo · 10 qualidade e aderência ao tema · **15 domínio do assunto**

## A tese da apresentação

> "Pegamos um sistema real (o Spring PetClinic, mantido pelo time do Spring) e o evoluímos com cada tópico da
> disciplina. Em cada um mostramos a tecnologia clássica e explicamos **por que a indústria a trocou** pela moderna."

O professor pede, no tópico 7, aplicação com padrões, multithreading, RMI e frameworks (7.1), banco de dados (7.2)
e UML (7.3). O card **"Mapa da disciplina"** do painel mostra onde cada item está. Abram a aula por ele e
fechem a prática por ele.

## Quem apresenta o quê (4 pessoas)

Cada pessoa fica com o **mesmo assunto nos dois dias**: explica na terça e demonstra na quinta. Assim cada um
estuda a fundo uma parte só, e a prática fecha o que a teoria abriu. São cerca de 17 minutos por pessoa em
cada dia.

| Pessoa | Assunto | Terça (teoria) | Quinta (prática) |
|---|---|---|---|
| **P1** | O sistema e a comunicação remota | Abertura, o sistema, genéricos, RMI → gRPC | Painel e deploy, genéricos ao vivo, RMI no terminal, gRPC |
| **P2** | Concorrência | Threads, sincronização, virtual threads, Observer + `@Async` | Benchmark, `/thread`, registro de visita com lembrete |
| **P3** | Projeto | SOLID, padrões (Strategy, Template Method, Builder...), AOP | Strategy + live coding, AOP com o console, galeria UML |
| **P4** | Frameworks e banco de dados | Spring MVC, JPA/Hibernate, Spring Data, PostgreSQL no Neon, relatórios, testes, fechamento | Card 7 (banco), relatório PDF, `./mvnw test`, fechamento |

O João, autor da UML, fica bem como **P3**: os diagramas de projeto (05, 06, 07) são dele e caem nesse bloco.
Todos precisam saber responder sobre todas as partes. A nota de domínio é do grupo.

**O pedido do professor (mais visual no código e no banco):**
- os cards do painel agora mostram **as linhas reais do código**, com o arquivo, o número das linhas e as linhas
  importantes marcadas com uma nota;
- o **card 7** mostra o banco por inteiro: o caminho de uma consulta, a classe ao lado da tabela, o modelo
  entidade-relacionamento, as linhas de cada tabela e o SQL que o Hibernate gera, ao vivo;
- regra para os slides e para a fala: **toda vez que citar uma classe, mostre o código dela**, e não só o nome.

---

## Terça: parte teórica (70 min)

| Min | Quem | Bloco | Conteúdo e o que mostrar |
|---|---|---|---|
| 0–4 | P1 | Abertura | O que é um estudo de caso; por que o PetClinic (aplicação de referência oficial do Spring); a tese acima; os itens 7.1–7.3; quem apresenta o quê |
| 4–10 | P1 | O sistema | Domínio (UML 01 e 02), arquitetura MVC + camadas (UML 03), implantação (UML 11). Stack: Java 21, Spring Boot 4.1, PostgreSQL no Neon, Docker no Render |
| 10–13 | P1 | Genéricos | `Repository<Vet, Integer>`, `Map<String, VisitPriceStrategy>`; *type erasure*; tipos brutos; curingas |
| 13–17 | P1 | RMI → gRPC | Stub, skeleton, Registry; limitações (só Java, serialização insegura, firewall); gRPC: contrato `.proto`, Protobuf + HTTP/2. Mostrar a interface `VetService` e o `vets.proto` lado a lado. Padrões **Proxy** e **Broker** |
| 17–30 | P2 | Multithreading | Estados de uma thread; thread de plataforma = thread do SO; pool fixo; `Semaphore`; `Callable`/`Future`; virtual threads desmontam da *carrier* ao bloquear. Mostrar o laço do `BenchmarkDemo` |
| 30–34 | P2 | Observer + `@Async` | Sujeito, evento e observador; por que a resposta HTTP não espera (UML 04). Mostrar a linha do `publishEvent` e o `@Async` do listener |
| 34–44 | P3 | Princípios e padrões | SOLID com o código (OCP no Strategy, ISP no `VetRepository`, DIP no publisher); Strategy (UML 05), Observer (UML 06), Template Method, Builder; anti-padrão: cadeia de `if/switch` |
| 44–51 | P3 | Aspectos | Espalhamento e entrelaçamento; pointcut, advice, join point, weaving; AspectJ × Spring AOP (UML 07). Mostrar o `AuditAspect` com as três partes marcadas |
| 51–57 | P4 | Frameworks | Spring MVC (sucessor do Struts): controller → template; injeção de dependência; JUnit 5 + MockMvc; JasperReports (layout `.jrxml` separado dos dados) |
| 57–66 | P4 | Banco de dados | JDBC à mão × ORM; `@Entity`, `@Table`, `@ManyToOne`, `@OneToMany` ao lado do `CREATE TABLE`; Spring Data: o repositório é só uma interface e a consulta sai do nome do método; JPQL com `@Query`; transação e rollback; pool HikariCP; o Neon (PostgreSQL gerenciado) e o H2 nos testes |
| 66–70 | P4 | Fechamento | Tabela Clássico × Moderno (inclui Struts → Spring MVC, JDBC → JPA e o caso Equifax); prévia da prática de quinta |

**Slides:** no máximo um por linha da tabela, com o diagrama UML ou o trecho de código correspondente.

---

## Quinta: parte prática (70 min)

Mantenham o **console ao vivo** do painel visível o tempo todo: é ele que prova que o código está rodando.

| Min | Quem | O que fazer | O que mostrar / dizer |
|---|---|---|---|
| 0–4 | P1 | Abrir o `/painel` | Cabeçalho: Java 21, Spring Boot 4.1, **PostgreSQL 18 (Neon)**, virtual threads ligadas. Card **Mapa da disciplina**. Deploy: GitHub → Docker → Render, banco no Neon. Abrir o site no celular para mostrar que é responsivo |
| 4–7 | P1 | Card 0 (Genéricos) | Botão "Inspecionar": `ArrayList == ArrayList → true`, mas o Spring lê `Map<String, VisitPriceStrategy>` e injeta 3 beans |
| 7–12 | P1 | RMI no terminal (`demos/rmi`) | `javac *.java`, `java Server`, `java Client`. Mostrar `extends Remote` e `throws RemoteException` |
| 12–17 | P1 | Card 1 (gRPC) | Os dois contratos lado a lado; chamada: **169 B em Protobuf × 447 B em JSON**; log `[gRPC]` no console |
| 17–26 | P2 | Benchmark | Terminal: `java BenchmarkThreads.java 2000` (cerca de 10 s × 1 s). Card 2: **2.000 tarefas** no servidor (≈ 10.000 ms × 1.000 ms) e `GET /thread` (carrier `ForkJoinPool`) |
| 26–34 | P2 | Card 3 (Observer) | Registrar visita: a resposta volta em menos de 0,5 s e "lembrete enviado" aparece **+2.000 ms** depois em `task-N`. Apontar a linha 118 do `VisitController` marcada no card |
| 34–43 | P3 | Card 4 (Strategy) + live coding | Tabela de preços; link "tela" da Rosy (**R$ 225,00**). **Live coding** (abaixo): estratégia do hamster, local |
| 43–48 | P3 | Card 5 (AOP) | Proxies (`VetController$$SpringCGLIB$$0`); `GET /vets` gera `[AUDITORIA]`; `GET /` não gera nada |
| 48–51 | P3 | Galeria UML | Rastrear diagrama → código (05 → `pricing`, 07 → `audit`) |
| 51–62 | P4 | Card 7 (Banco) | Ler o caminho da consulta e a classe ao lado da tabela (①–④). **Contar linhas**. Clicar nas quatro chamadas: `findById` traz dono, pets e visitas em 2 comandos; `findByLastNameStartingWith` vira `like ?` sem ninguém ter escrito SQL; `@Query` em JPQL vira SQL; `INSERT + rollback`: contar as linhas de novo e mostrar que `types` não mudou. Depois, registrar uma visita no card 3 e contar: `visits` aumenta |
| 62–65 | P4 | Card 6 (Relatório) + testes | PDF com pets por tipo e preço (total **R$ 1.640,00**). Terminal: `./mvnw test` → **108 testes, 0 falhas** |
| 65–70 | todos | Perguntas | Usar a lista de perguntas prováveis abaixo |

### Live coding: Strategy e o princípio Aberto/Fechado (cerca de 3 min, local)

Na máquina, com `./mvnw spring-boot:run` (H2): o DevTools reinicia sozinho ao salvar. Crie
`src/main/java/org/springframework/samples/petclinic/pricing/HamsterPriceStrategy.java`:

```java
package org.springframework.samples.petclinic.pricing;

import java.math.BigDecimal;

import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.stereotype.Component;

@Component("hamster")
class HamsterPriceStrategy implements VisitPriceStrategy {

	@Override
	public BigDecimal calculatePrice(Pet pet) {
		return new BigDecimal("45.00");
	}

}
```

Recarregue `http://localhost:8080/painel#ext4`: o Basil (hamster) passa de `DefaultPriceStrategy` R$ 70,00 para
`HamsterPriceStrategy` **R$ 45,00**. **Nenhuma classe existente foi alterada**, e isso é o OCP na prática.
Apaguem o arquivo depois: ele quebraria o teste que espera 3 estratégias.

---

## Checklist

**Na véspera**
- [ ] Ensaiar com cronômetro os dois dias (a nota de tempo vale 10 pts)
- [ ] Clicar em todos os botões do painel no Render
- [ ] Testar o live coding localmente e conferir `java -version` = 21
- [ ] Gravar um vídeo da prática como plano B

**No dia**
- [ ] Abrir o painel **5 a 10 minutos antes**: o plano free do Render dorme, e a primeira resposta pode levar mais de 1 minuto
- [ ] Deixar dois terminais abertos em `demos/rmi` e `demos/threads`, e rodar `chcp 65001` (acentos no PowerShell)
- [ ] Plano B sem internet: `./mvnw spring-boot:run` e `http://localhost:8080/painel` (sem os diagramas Mermaid, que vêm de CDN)

---

## Perguntas prováveis (domínio do assunto, 15 pts)

**Por que gRPC e não REST/JSON?** Contrato tipado e gerado (`.proto`), mensagem binária menor (62% menor na demo),
HTTP/2 com multiplexação e streaming. REST continua melhor para APIs públicas e navegadores, e é por isso que o
painel conversa em HTTP e o gRPC fica entre serviços.

**Que protocolo o RMI usa?** JRMP sobre TCP, com serialização Java. O Registry (porta 1099) é o serviço de naming,
e o stub é um **Proxy** do objeto remoto.

**Virtual thread deixa código CPU-bound mais rápido?** Não. Ela ganha em tarefas que **esperam** (I/O, banco, rede).
Com CPU cheia o gargalo é o número de núcleos. No Java 21, um bloco `synchronized` "prende" (*pinning*) a virtual
thread à carrier; o JDK 24 (JEP 491) resolveu isso.

**Virtual threads acabam com a programação reativa?** Para a maioria das aplicações bloqueantes, tornam-na
desnecessária: o código continua simples e escala. A reativa ainda faz sentido para *backpressure* e *streaming*.

**O evento é publicado antes ou depois de gravar no banco?** Depois: o `save` termina (e faz o commit) antes do
`publishEvent`. Se o controller estivesse dentro de uma transação, o certo seria `@TransactionalEventListener`
(AFTER_COMMIT), para não avisar sobre algo que pode sofrer rollback.

**Strategy não é só um `Map` de preços?** Não: cada estratégia é um **algoritmo** (o do cão usa a idade). Um mapa de
números não teria comportamento. A diferença para o State é que, no Strategy, quem escolhe é o cliente ou o
contexto, e não o próprio objeto mudando de estado.

**Limitações do Spring AOP em relação ao AspectJ?** Só intercepta métodos de beans do Spring chamados **através do
proxy**: uma chamada interna via `this` não passa pelo aspecto. Também não intercepta atribuição de campos nem
construtores. O AspectJ (`ajc` ou *load-time weaving*) faz tudo isso, ao custo de um build mais complexo.

**O que é type erasure e qual a consequência?** O compilador apaga os parâmetros de tipo, então em tempo de execução
`List<String>` e `List<Integer>` são `ArrayList`. Por isso não existe `new T()` nem `instanceof List<String>`.
O tipo continua nas assinaturas, que é o que o Spring lê (demo no card 0).

**Onde está o Hibernate?** Atrás do JPA: as entidades (`@Entity`) e os repositórios do Spring Data, que nem têm
classe de implementação (são proxies gerados). O SQL é do Hibernate; o banco é o PostgreSQL no Neon.

**Por que H2 nos testes e PostgreSQL no site?** Testes rápidos, isolados e sem rede. O profile `postgres` troca
só a configuração, sem mudar código (o JPA abstrai o banco).

**O que é o caso Equifax?** Em 2017, uma falha conhecida e já corrigida do Apache Struts (CVE-2017-5638) não foi
atualizada e expôs dados de cerca de 147 milhões de pessoas. É um argumento a favor de frameworks mantidos e
atualizados.

**Como funciona o relatório (JasperReports, item 4.4)?** Três passos: **compilar** o layout `.jrxml` (o arquivo
que o iReport, hoje Jaspersoft Studio, edita visualmente) em um `JasperReport`; **preencher** com os dados
(`JRBeanCollectionDataSource`, que lê os getters de `ReportRow`); **exportar** para PDF. O layout tem
`parameter`, `field`, `variable` (somas) e bandas (title, detail, groupHeader/groupFooter, summary). O relatório
não calcula preço: recebe as linhas prontas do `VisitPriceService` (Strategy). Compilamos uma vez e guardamos.

**O Jasper é "clássico" ou "moderno"?** O motor continua sendo o padrão no Java (versão 7, de 2025). O que ficou
para trás foi o editor iReport, substituído pelo Jaspersoft Studio, e o formato antigo do `.jrxml`.

**Onde está a implementação do `OwnerRepository`?** Não existe no código. O Spring Data cria um proxy em tempo de
execução que implementa a interface; `findByLastNameStartingWith` vira consulta pelo nome do método. É o padrão
**Proxy** de novo, e o card 0 mostra a classe gerada.

**O que é o problema N+1 e ele aparece aqui?** Uma consulta para a lista e mais uma para cada item. Os pets e as
visitas são EAGER, e o `default_batch_fetch_size=16` faz o Hibernate buscar as visitas de até 16 pets num comando
só, em vez de um comando por pet. O card 7 mostra quantos comandos cada chamada gerou.

**Por que os testes não usam o Neon?** São rápidos, não dependem de rede e não sujam o banco da apresentação. O
mesmo código roda nos dois porque o JPA esconde o dialeto: só mudam a URL e o profile.

**O que garante que o `INSERT + rollback` não grava nada?** A transação do `TransactionTemplate` é marcada com
`setRollbackOnly()`. O `INSERT` chega ao banco (`saveAndFlush`), mas o PostgreSQL desfaz tudo no fim. O id
gerado é consumido mesmo assim, por isso aparece um número novo a cada clique.

**Vocês desenvolveram o sistema?** O núcleo é o PetClinic original. As cinco extensões, o painel, o deploy e a UML
são do grupo, e o histórico do Git mostra uma branch e commits por extensão. O README da UML já faz essa distinção.

---

## Pendências do grupo

- [x] **UML:** o 03 mostra o PostgreSQL (Neon) e o componente de relatórios; o 09 ficou como "execução local";
  novo diagrama **11 (produção: Render + Neon)**. Também foram corrigidos os estereótipos `<<<<x>>>>` (05, 06, 07)
  e o `viewBox` dos SVGs do Graphviz, que no navegador mostravam só parte do diagrama. Detalhes no histórico de
  revisões do `docs/uml/README.md`. **João, confira se concorda com as mudanças.**
- [x] **Relatórios (item 4.4):** implementado com JasperReports (card 6 do painel).
- [ ] **Data da apresentação:** o cronograma marca "Estudo de caso" nas semanas 9 (27 e 29/10) e 11 (10 e 12/11).
  Confirmar com o professor.
- [ ] **Senha do Neon:** trocar depois da apresentação, porque ela circulou em conversas.
- [ ] **Render:** após cada push, fazer **Manual Deploy → Deploy latest commit**. O serviço foi criado a partir
  da URL pública do repositório e não faz deploy automático.
