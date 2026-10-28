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

## Quem apresenta o quê (sugestão)

| Pessoa | Teoria (terça) | Prática (quinta) |
|---|---|---|
| **P1** | Abertura, o sistema, RMI → gRPC, genéricos | Painel e deploy, RMI no terminal, gRPC, genéricos ao vivo |
| **P2** | Multithreading → virtual threads, Observer + `@Async` | Benchmark, `/thread`, registro de visita |
| **P3 (João, autor da UML)** | Princípios e padrões, AOP, UML | Strategy + live coding, AOP, UML, testes, fechamento |

Todos precisam saber responder sobre todas as partes. A nota de domínio é do grupo.

---

## Terça: parte teórica (70 min)

| Min | Quem | Bloco | Conteúdo e o que mostrar |
|---|---|---|---|
| 0–5 | P1 | Abertura | O que é um estudo de caso; por que o PetClinic (aplicação de referência oficial do Spring, mantida pelo time do framework); a tese acima; os itens 7.1–7.3 |
| 5–15 | P1 | O sistema | Domínio (UML 01 e 02), arquitetura MVC + camadas (UML 03), implantação (UML 11). Frameworks do tópico 4: Spring MVC (sucessor do Struts), Hibernate/JPA, JUnit, JasperReports. Stack: Java 21, Spring Boot 4.1, PostgreSQL no Neon, Docker no Render |
| 15–27 | P1 | RMI → gRPC | Camadas do RMI (stub, skeleton, Registry/naming); limitações (só Java, serialização Java insegura, firewall); gRPC = mesmo conceito de stub, contrato `.proto` neutro, Protobuf + HTTP/2. Padrões **Proxy** (stub) e **Broker** |
| 27–32 | P1 | Genéricos | Classes e métodos genéricos no código (`Repository<Vet, Integer>`); *type erasure*; tipos brutos; curingas |
| 32–45 | P2 | Multithreading | Estados de uma thread; thread de plataforma = thread do SO; pool fixo; sincronização (`Semaphore`); `Callable`/`Future`; virtual threads (Java 21): desmontam da *carrier* ao bloquear |
| 45–50 | P2 | Observer + `@Async` | Sujeito, evento e observador; produtor/consumidor; por que a resposta HTTP não espera (UML 04) |
| 50–60 | P3 | Princípios e padrões | SOLID com exemplos do código (OCP no Strategy, ISP no `VetRepository`, DIP no publisher); Strategy (UML 05), Observer (UML 06), Template Method, Builder; anti-padrão: cadeia de `if/switch` |
| 60–67 | P3 | Aspectos | Espalhamento e entrelaçamento; concern, pointcut, advice, join point, weaving; AspectJ (`ajc`) × Spring AOP (proxy CGLIB) (UML 07) |
| 67–70 | P3 | Fechamento | Tabela Clássico × Moderno (inclui Struts → Spring MVC e o caso Equifax); prévia da prática de quinta |

**Slides:** um por linha da tabela, no máximo, com o diagrama UML correspondente. A tabela "Clássico vs Moderno" do
`ESTUDO-DE-CASO.md` (seção 7) vira o slide de fechamento.

---

## Quinta: parte prática (70 min)

Mantenham o **console ao vivo** do painel visível o tempo todo: é ele que prova que o código está rodando.

| Min | Quem | O que fazer | O que mostrar / dizer |
|---|---|---|---|
| 0–5 | P1 | Abrir o `/painel` | Cabeçalho: Java 21, Spring Boot 4.1, **PostgreSQL 18 (Neon)**, virtual threads ligadas. Card **Mapa da disciplina**. Explicar o deploy: GitHub → Docker → Render, banco no Neon |
| 5–15 | P1 | RMI no terminal (`demos/rmi`) | `javac *.java`, `java Servidor`, `java Cliente`. Mostrar `extends Remote` e `throws RemoteException` |
| 15–22 | P1 | Card 1 (gRPC) | Os dois contratos lado a lado; botão de chamada: **169 B em Protobuf × 447 B em JSON**; log `[gRPC]` no console |
| 22–25 | P1 | Card 0 (Genéricos) | Botão "Inspecionar": `ArrayList == ArrayList → true`, mas o Spring lê `Map<String, PrecoConsultaStrategy>` e injeta 3 beans |
| 25–33 | P2 | Benchmark | Terminal: `java BenchmarkThreads.java 2000` (cerca de 10 s × 1 s). Card 2: rodar **2.000 tarefas** no servidor (**≈ 10.000 ms × 1.000 ms**) e clicar em `GET /thread` (carrier `ForkJoinPool`) |
| 33–42 | P2 | Card 3 (Observer) | Registrar visita: a resposta volta em menos de 0,5 s e "lembrete enviado" aparece **+2.000 ms** depois em `task-N`. Abrir `VisitController` e mostrar a única linha nova, o `publishEvent` |
| 42–50 | P3 | Card 4 (Strategy) + live coding | Tabela de preços; link "tela" da Rosy (**R$ 225,00**). **Live coding** (abaixo): criar a estratégia do hamster, local |
| 50–55 | P3 | Card 5 (AOP) | Tabela de proxies (`VetController$$SpringCGLIB$$0`); `GET /vets` gera `[AUDITORIA]`; `GET /` não gera nada. Abrir `AuditoriaAspect` |
| 55–59 | P3 | Card 6 (Relatório) | "Gerar relatório PDF": pets agrupados por tipo, estratégia e preço (total **R$ 1.640,00**). O console mostra o `[AUDITORIA]` do relatório: cerca de 1 s no Render (o `.jrxml` é compilado quando a aplicação sobe). Abrir `pets-e-precos.jrxml` |
| 59–63 | P3 | Galeria UML + testes | Card UML (11 diagramas): rastrear diagrama → código. Terminal: `./mvnw test` → **103 testes, 0 falhas** |
| 63–70 | todos | Perguntas | Usar a lista de perguntas prováveis abaixo |

### Live coding: Strategy e o princípio Aberto/Fechado (cerca de 3 min, local)

Na máquina, com `./mvnw spring-boot:run` (H2): o DevTools reinicia sozinho ao salvar. Crie
`src/main/java/org/springframework/samples/petclinic/preco/PrecoConsultaHamster.java`:

```java
package org.springframework.samples.petclinic.preco;

import java.math.BigDecimal;

import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.stereotype.Component;

@Component("hamster")
class PrecoConsultaHamster implements PrecoConsultaStrategy {

	@Override
	public BigDecimal calcularPreco(Pet pet) {
		return new BigDecimal("45.00");
	}

}
```

Recarregue `http://localhost:8080/painel#ext4`: o Basil (hamster) passa de `PrecoConsultaPadrao` R$ 70,00 para
`PrecoConsultaHamster` **R$ 45,00**. **Nenhuma classe existente foi alterada**, e isso é o OCP na prática.
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
(`JRBeanCollectionDataSource`, que lê os getters de `LinhaRelatorio`); **exportar** para PDF. O layout tem
`parameter`, `field`, `variable` (somas) e bandas (title, detail, groupHeader/groupFooter, summary). O relatório
não calcula preço: recebe as linhas prontas do `PrecoConsultaService` (Strategy). Compilamos uma vez e guardamos.

**O Jasper é "clássico" ou "moderno"?** O motor continua sendo o padrão no Java (versão 7, de 2025). O que ficou
para trás foi o editor iReport, substituído pelo Jaspersoft Studio, e o formato antigo do `.jrxml`.

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
