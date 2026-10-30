/*
 * Case study dashboard (/painel). Calls the dashboard API (/api/dashboard/*) and the real
 * PetClinic endpoints (/thread, the visit form, /owners/1, /vets) so aspects, events and
 * virtual threads run for real. User-facing text is in Portuguese.
 */
(function () {
  'use strict';

  const root = document.getElementById('dashboard');

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (c) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  }

  async function getJson(url) {
    const response = await fetch(url, { headers: { Accept: 'application/json' } });
    if (!response.ok) {
      throw new Error('HTTP ' + response.status + ' em ' + url);
    }
    return response.json();
  }

  function currency(value) {
    return Number(value).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  function shortTime(instant) {
    const d = new Date(instant);
    return d.toLocaleTimeString('pt-BR', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0');
  }

  function showError(element, error) {
    element.innerHTML = '<div class="alert alert-danger py-2 mb-0">' + escapeHtml(error.message || error) + '</div>';
  }

  function truncate(text, max) {
    return text.length > max ? text.slice(0, max) + '…' : text;
  }

  function sourceFiles(files) {
    return '<p class="dash-files"><b>No código:</b> ' + files.map((f) => '<code>' + escapeHtml(f) + '</code>').join(' ') + '</p>';
  }

  function card(id, number, title, classic, modern, why, left, right, files) {
    return `
      <section class="dash-card" id="${id}">
        <h2><span class="dash-num">${number}</span> ${title}
          <span class="dash-tag"><span class="dash-classic">${classic}</span> &rarr; <b>${modern}</b></span>
        </h2>
        <p class="dash-why">${why}</p>
        <div class="dash-body">
          <div class="dash-box">${left}</div>
          <div class="dash-box dash-demo">${right}</div>
        </div>
        ${sourceFiles(files)}
      </section>`;
  }

  function diagram(title, mermaidSource) {
    return '<h3>' + title + '</h3><div class="dash-diagram"><pre class="mermaid">' + escapeHtml(mermaidSource) + '</pre></div>';
  }

  // Real excerpts from the repository: file name, line numbers and marked lines with a note
  const KEYWORDS = {
    java: 'package import public private protected static final class interface record extends implements return new try finally throws throw if else for while void int long boolean this null var default case switch',
    sql: 'select from where join left inner outer on and or order by group insert into values create table if exists references primary key not null int text date unique index as like offset fetch first rows only limit generated default identity asc desc count in',
    proto: 'syntax package option service rpc returns message repeated string int32',
    props: ''
  };

  function highlightLine(line, lang) {
    const words = new Set(KEYWORDS[lang].split(' '));
    const comment = { sql: '--.*', props: '#.*' }[lang] || '//.*';
    const re = new RegExp('(' + comment + ')|("(?:[^"\\\\]|\\\\.)*")|(@[A-Za-z]+)|([A-Za-z_][A-Za-z0-9_]*)|(\\d[\\d_]*)', 'g');
    let out = '';
    let last = 0;
    let m;
    while ((m = re.exec(line)) !== null) {
      out += escapeHtml(line.slice(last, m.index));
      const token = escapeHtml(m[0]);
      if (m[1]) {
        out += '<span class="c">' + token + '</span>';
      }
      else if (m[2]) {
        out += '<span class="s">' + token + '</span>';
      }
      else if (m[3]) {
        out += '<span class="a">' + token + '</span>';
      }
      else if (m[4] && words.has(lang === 'sql' ? m[4].toLowerCase() : m[4])) {
        out += '<span class="k">' + token + '</span>';
      }
      else if (m[4] && lang !== 'sql' && /^[A-Z]/.test(m[4])) {
        out += '<span class="t">' + token + '</span>';
      }
      else if (m[5]) {
        out += '<span class="n">' + token + '</span>';
      }
      else {
        out += token;
      }
      last = re.lastIndex;
    }
    return out + escapeHtml(line.slice(last));
  }

  // marks: { lineNumber: 'note' }; an empty note only highlights the line
  function source(file, lang, start, code, marks = {}) {
    const body = code.replace(/^\n+|\s+$/g, '').split('\n').map((line, i) => {
      const n = start + i;
      const marked = Object.prototype.hasOwnProperty.call(marks, n);
      return '<span class="dash-line' + (marked ? ' mark' : '') + '"><span class="ln">' + n + '</span>' +
        highlightLine(line, lang) + (marks[n] ? '<span class="note">' + marks[n] + '</span>' : '') + '</span>';
    }).join('');
    return '<figure class="dash-src"><figcaption>' + escapeHtml(file) + '</figcaption><pre><code>' + body + '</code></pre></figure>';
  }

  function formatSql(sql) {
    return sql.replace(/\s+(from|where|left join|join|order by|offset|fetch|values|limit)\s/g, '\n$1 ')
      .replace(/,\s*(?=[a-z]\d+_\d+\.)/g, ',\n    ');
  }

  function table(header, rows) {
    return '<table class="dash-table"><tr>' + header.map((c) => '<th>' + c + '</th>').join('') + '</tr>' +
      rows.map((r) => '<tr>' + r.map((c) => '<td>' + c + '</td>').join('') + '</tr>').join('') + '</table>';
  }

  // ---------------------------------------------------------------------------
  // Mermaid diagrams
  // ---------------------------------------------------------------------------

  // TB stacks the blocks, "direction LR" keeps each flow horizontal, ~~~ keeps the order
  const GRPC_DIAGRAM = `flowchart TB
  subgraph C["RMI (clássico)"]
    direction LR
    CR["Cliente Java"] -- "serialização Java<br/>porta 1099 + dinâmicas" --> SR["Servidor Java"]
  end
  subgraph M["gRPC (moderno)"]
    direction LR
    CG["Cliente em qualquer<br/>linguagem (stub do .proto)"] -- "Protobuf / HTTP/2<br/>porta 9090" --> SG["VetGrpcService"]
    SG --> RG[("VetRepository<br/>PostgreSQL")]
  end
  C ~~~ M`;

  const THREADS_DIAGRAM = `flowchart TB
  subgraph P["Pool fixo: 200 threads do SO"]
    direction LR
    T1["10.000 tarefas"] --> F["fila de espera"] --> W["200 threads<br/>bloqueadas no sleep"]
  end
  subgraph V["Virtual threads"]
    direction LR
    T2["10.000 virtual threads"] -- "bloqueou? é desmontada" --> CT["poucas carrier threads<br/>(ForkJoinPool)"]
  end
  P ~~~ V`;

  const OBSERVER_DIAGRAM = `sequenceDiagram
  autonumber
  actor U as Navegador
  participant VC as VisitController
  participant P as ApplicationEventPublisher
  participant L as VisitReminderListener
  U->>VC: POST visita (virtual thread tomcat-handler)
  VC->>VC: owners.save(owner)
  VC->>P: publishEvent(VisitRegisteredEvent)
  P-->>L: @Async: entrega em outra thread (task-N)
  VC-->>U: 302 Redirect (não espera o lembrete)
  Note over L: sleep 2 s (simula e-mail/SMS)
  L->>L: log "lembrete enviado"`;

  const STRATEGY_DIAGRAM = `classDiagram
  direction LR
  class VisitPriceStrategy {
    <<interface>>
    +calculatePrice(Pet) BigDecimal
  }
  class VisitPriceService {
    -Map~String,VisitPriceStrategy~ strategiesByType
    +calculatePrice(Pet) BigDecimal
  }
  VisitPriceStrategy <|.. DogPriceStrategy : bean "dog"
  VisitPriceStrategy <|.. CatPriceStrategy : bean "cat"
  VisitPriceStrategy <|.. DefaultPriceStrategy : bean "default"
  VisitPriceService o-- VisitPriceStrategy : Spring injeta todas`;

  const AOP_DIAGRAM = `flowchart LR
  R["Requisição HTTP"] --> PX["Proxy CGLIB<br/>VetController$$SpringCGLIB"]
  PX --> A["AuditAspect<br/>@Around('@annotation(audited)')"]
  A -- "proceed()" --> M["VetController.showResourcesVetList()<br/>(código original)"]
  M -- "retorno" --> A
  A -- "log: método, args, ms" --> LOG[("log")]`;

  const DB_DIAGRAM = `flowchart LR
  C["OwnerController"] --> R["OwnerRepository<br/>(só a interface)"]
  R --> SD["proxy do Spring Data<br/>implementa os métodos"]
  SD --> H["Hibernate (JPA)<br/>objeto ⇄ linha"]
  H --> J["JDBC + pool HikariCP"]
  J --> DB[("PostgreSQL 18<br/>Neon, São Paulo")]`;

  const ER_DIAGRAM = `erDiagram
  OWNERS ||--o{ PETS : "owner_id"
  TYPES ||--o{ PETS : "type_id"
  PETS ||--o{ VISITS : "pet_id"
  VETS ||--o{ VET_SPECIALTIES : "vet_id"
  SPECIALTIES ||--o{ VET_SPECIALTIES : "specialty_id"
  OWNERS {
    int id PK
    text first_name
    text last_name
    text city
  }
  PETS {
    int id PK
    text name
    date birth_date
    int type_id FK
    int owner_id FK
  }
  VISITS {
    int id PK
    int pet_id FK
    date visit_date
    text description
  }
  TYPES {
    int id PK
    text name
  }`;

  const SQL_DEMOS = {
    findById: {
      label: 'owners.findById(1)',
      code: () => source('owner/OwnerRepository.java', 'java', 36,
        'public interface OwnerRepository extends JpaRepository<Owner, Integer> {',
        { 36: 'findById, save, delete... vêm prontos' }),
      why: 'Método herdado do <code>JpaRepository</code>. Como <code>Owner.pets</code> e <code>Pet.visits</code> são EAGER, o Hibernate também traz os pets e as visitas.'
    },
    findByLastName: {
      label: 'findByLastNameStartingWith("Davis")',
      code: () => source('owner/OwnerRepository.java', 'java', 45,
        '\tPage<Owner> findByLastNameStartingWith(String lastName, Pageable pageable);',
        { 45: 'LastName + StartingWith → LIKE ?' }),
      why: 'Ninguém escreveu essa consulta: o Spring Data a deduz do <b>nome do método</b> e o Hibernate gera o SQL com paginação.'
    },
    query: {
      label: '@Query: findPetTypes()',
      code: () => source('owner/PetTypeRepository.java', 'java', 36, `
	@Query("SELECT ptype FROM PetType ptype ORDER BY ptype.name")
	List<PetType> findPetTypes();`, { 36: 'JPQL: fala de classes, não de tabelas' }),
      why: 'Com <code>@Query</code> a consulta é escrita em JPQL, sobre a classe <code>PetType</code>. O Hibernate a traduz para o SQL do banco em uso.'
    },
    rollback: {
      label: 'INSERT + rollback',
      code: () => source('dashboard/DatabaseDemoController.java', 'java', 91, `
	private List<?> insertAndRollBack() {
		return this.transactions.execute(status -> {
			PetType type = new PetType();
			type.setName("demo-rollback");
			PetType saved = this.petTypes.saveAndFlush(type);
			status.setRollbackOnly();
			return List.of(saved);
		});
	}`, { 95: 'o INSERT vai para o banco agora', 96: 'no fim da transação: desfaz' }),
      why: 'Um objeto novo vira um <code>INSERT</code> e ganha o id gerado pelo banco. A transação é desfeita no fim, então nenhuma linha fica gravada: conte as linhas antes e depois.'
    }
  };

  const REPORT_DIAGRAM = `flowchart LR
  J[".jrxml<br/>(layout do iReport /<br/>Jaspersoft Studio)"] -- "1. compilar<br/>(uma vez)" --> R["JasperReport"]
  D[("Pets do banco +<br/>preço do Strategy")] -- "JRBeanCollectionDataSource" --> F
  R --> F["2. preencher<br/>JasperPrint"]
  F -- "3. exportar" --> P["PDF"]`;

  // ---------------------------------------------------------------------------
  // Page skeleton
  // ---------------------------------------------------------------------------

  root.innerHTML = `
    <div class="dash-hero">
      <h1>Estudo de caso: Spring PetClinic</h1>
      <p>POO2 (UFU): cinco extensões, cada uma comparando a tecnologia clássica da disciplina com o seu equivalente moderno.
         Os botões desta página chamam a aplicação real, rodando agora.</p>
      <div id="runtime"><span class="dash-chip">carregando ambiente...</span></div>
    </div>

    <nav class="dash-nav">
      <a href="#syllabus">Mapa da disciplina</a>
      <a href="#ext1">1. RMI &rarr; gRPC</a>
      <a href="#ext2">2. Virtual Threads</a>
      <a href="#ext3">3. Observer + @Async</a>
      <a href="#ext4">4. Strategy</a>
      <a href="#ext5">5. AOP @Audited</a>
      <a href="#ext6">6. Relatório PDF</a>
      <a href="#ext7">7. Banco de dados</a>
      <a href="#uml">UML</a>
    </nav>

    <div class="dash-grid">
      <div id="cards"></div>
      <aside class="dash-console">
        <header>
          <span>Console: logs das extensões</span>
          <button type="button" id="clear-console">limpar</button>
        </header>
        <div class="lines" id="console"><div class="dash-empty">Clique nos botões ao lado. Os logs do aspecto, do listener e do gRPC aparecem aqui.</div></div>
      </aside>
    </div>`;

  const cards = document.getElementById('cards');

  // ---------- Syllabus map ----------

  const TOPICS = [
    ['1. Programação genérica', '<code>Repository&lt;Vet, Integer&gt;</code>, <code>Map&lt;String, VisitPriceStrategy&gt;</code>, <code>StreamObserver&lt;T&gt;</code>', 'demo logo abaixo'],
    ['2. Multithreading', 'virtual threads, <code>ExecutorService</code>, tarefas <code>Callable</code>/<code>Future</code>, <code>Semaphore</code> (sincronização), <code>@Async</code>', '<a href="#ext2">card 2</a>, <a href="#ext3">card 3</a>'],
    ['3. RMI', '<code>demos/rmi</code>: interface <code>Remote</code>, stub, Registry :1099 &rarr; sucessor gRPC', '<a href="#ext1">card 1</a>'],
    ['4. Frameworks (MVC, dados, testes, relatórios)', 'Spring MVC (sucessor do Struts), JPA/Hibernate, JUnit 5 (108 testes), JasperReports (PDF)', '<a href="#ext6">card 6</a>, <a href="#ext7">card 7</a> e todo o sistema'],
    ['5. Princípios e padrões', 'Strategy, Observer, Proxy, Template Method, Builder, MVC, Broker... e SOLID', '<a href="#ext3">3</a>, <a href="#ext4">4</a>, <a href="#ext5">5</a> e tabela abaixo'],
    ['6. Aspectos', 'concern, pointcut, advice, join point, weaving: <code>@Audited</code> + <code>AuditAspect</code>', '<a href="#ext5">card 5</a>'],
    ['7.1 Aplicação integrada', 'as 5 extensões rodando juntas no mesmo PetClinic', 'esta página'],
    ['7.2 Persistência', 'PostgreSQL no Neon via JPA/Hibernate e Spring Data', '<a href="#ext7">card 7</a>'],
    ['7.3 UML', '11 diagramas: casos de uso, classes, componentes, sequência, implantação, atividade', '<a href="#uml">galeria UML</a>']
  ];

  const PATTERNS = [
    ['Strategy', 'GoF comportamental', '<code>pricing/*PriceStrategy</code>', 'algoritmo de preço trocável, escolhido por um Map injetado'],
    ['Observer', 'GoF comportamental', '<code>reminder/*</code> + <code>ApplicationEventPublisher</code>', 'o sujeito publica; os observadores reagem sem serem conhecidos'],
    ['Template Method', 'GoF comportamental', '<code>DashboardLogAppender extends AppenderBase</code>', '<code>doAppend()</code> é o esqueleto fixo; nós só preenchemos <code>append()</code>'],
    ['Command', 'GoF comportamental', 'tarefas enviadas ao <code>ExecutorService</code> (<code>BenchmarkDemo</code>)', 'a ação vira objeto e roda depois, em outra thread'],
    ['Proxy', 'GoF estrutural', 'stubs RMI/gRPC, proxies CGLIB do AOP, <code>VetRepository</code> sem implementação', 'um substituto controla o acesso ao objeto real'],
    ['Adapter', 'GoF estrutural', '<code>VetGrpcService.toMessage()</code>', 'converte a entidade JPA <code>Vet</code> na mensagem Protobuf'],
    ['Builder', 'GoF criacional', '<code>VetInfo.newBuilder()...build()</code>, <code>ManagedChannelBuilder</code>', 'monta objetos imutáveis passo a passo'],
    ['Factory Method', 'GoF criacional', '<code>Executors.newVirtualThreadPerTaskExecutor()</code>, <code>newBlockingStub()</code>', 'quem chama não sabe qual classe concreta é criada'],
    ['Singleton', 'GoF criacional', 'beans do Spring (escopo padrão)', 'uma instância por contexto, garantida pelo container'],
    ['MVC / Layers', 'arquitetural', 'controllers &rarr; repositories &rarr; banco; views Thymeleaf', 'camadas com dependência em um só sentido'],
    ['Broker', 'arquitetural', 'RMI Registry / gRPC', 'cliente e servidor se comunicam por um intermediário de rede']
  ];

  const SOLID = [
    ['SRP', '<code>VisitReminderListener</code> só envia lembrete; <code>AuditAspect</code> só audita'],
    ['OCP', 'tipo de pet novo = classe nova <code>@Component("hamster")</code>, sem editar nada'],
    ['LSP', 'qualquer <code>VisitPriceStrategy</code> substitui outra no serviço'],
    ['ISP', '<code>VetRepository</code> estende o marcador <code>Repository</code> e expõe só <code>findAll</code>, não os ~20 métodos de <code>JpaRepository</code>'],
    ['DIP', '<code>VisitController</code> depende de <code>ApplicationEventPublisher</code> (abstração), nunca do listener'],
    ['ADP (pacotes)', 'sem ciclos: <code>dashboard</code> &rarr; extensões &rarr; núcleo; nenhuma extensão depende do painel']
  ];

  cards.insertAdjacentHTML('beforeend', `
    <section class="dash-card" id="syllabus">
      <h2><span class="dash-num">0</span> Mapa da disciplina <span class="dash-tag">plano de ensino &rarr; código</span></h2>
      <p class="dash-why">O tópico 7 pede uma aplicação que use <b>princípios e padrões, multithreading, RMI e frameworks</b> (7.1),
        com <b>persistência em banco</b> (7.2) e <b>documentada em UML</b> (7.3). Cada linha aponta onde isso aparece neste sistema.</p>
      <div class="dash-body">
        <div class="dash-box"><h3>Tópicos do plano de ensino</h3>${table(['Tópico', 'Onde aparece', 'Ver'], TOPICS)}</div>
        <div class="dash-box dash-demo">
          <h3>Demo ao vivo: genéricos e <i>type erasure</i></h3>
          <div class="dash-actions"><button class="btn btn-primary btn-sm" id="generics">Inspecionar genéricos em tempo de execução</button></div>
          <div class="dash-result" id="generics-result"><span class="dash-empty">O tipo genérico some dos objetos, mas continua nas assinaturas, e o Spring o lê para injetar dependências.</span></div>
        </div>
        <div class="dash-box"><h3>Padrões de projeto e arquiteturais no código</h3>${table(['Padrão', 'Categoria', 'Onde', 'O que mostrar'], PATTERNS)}</div>
        <div class="dash-box"><h3>Princípios SOLID e de pacotes</h3>${table(['Princípio', 'Exemplo no código'], SOLID)}
          <p class="small text-muted mt-2 mb-0"><b>Anti-padrões evitados:</b> cadeia de <code>if/switch</code> por tipo (&rarr; Strategy) e log espalhado em todos os métodos, o <i>scattering/tangling</i> (&rarr; AOP).</p>
        </div>
      </div>
    </section>`);

  // ---------- 1. RMI -> gRPC ----------
  cards.insertAdjacentHTML('beforeend', card('ext1', 1, 'Chamada remota de métodos', 'RMI', 'gRPC',
    'O RMI só conversa Java com Java, usa serialização Java (histórico de falhas graves de segurança) e portas que firewalls bloqueiam. ' +
    'O gRPC mantém a ideia de "chamar um método remoto por um stub", mas com contrato <code>.proto</code> neutro de linguagem, ' +
    'mensagens binárias Protobuf e transporte HTTP/2.',
    diagram('Antes e depois', GRPC_DIAGRAM) + `
      <h3 class="mt-3">O contrato</h3>
      ${source('demos/rmi/VetService.java', 'java', 5, `
public interface VetService extends Remote {

	List<String> listVets() throws RemoteException;

}`, { 5: 'contrato em Java: só Java chama', 7: 'toda chamada pode falhar na rede' })}
      ${source('src/main/protobuf/vets.proto', 'proto', 9, `
service VetService {
  rpc ListVets (ListVetsRequest) returns (ListVetsResponse);
}`, { 10: 'o stub de cada linguagem é gerado daqui' })}`,
    `<h3>Demo ao vivo</h3>
     <div class="dash-actions">
       <button class="btn btn-primary btn-sm" id="grpc">Chamar ListVets via gRPC</button>
     </div>
     <div class="dash-result" id="grpc-result"><span class="dash-empty">O servidor abre um canal HTTP/2 até a porta gRPC e chama o stub gerado.</span></div>
     <p class="small text-muted mt-2 mb-0">A demo de RMI puro roda só localmente: <code>cd demos/rmi</code>, <code>javac *.java</code>, <code>java Server</code> e, em outro terminal, <code>java Client</code>.</p>`,
    ['demos/rmi/*.java', 'src/main/protobuf/vets.proto', 'grpc/VetGrpcService.java', 'grpc/client/GrpcClient.java']));

  // ---------- 2. Virtual threads ----------
  cards.insertAdjacentHTML('beforeend', card('ext2', 2, 'Concorrência', 'Threads de plataforma', 'Virtual Threads',
    'Cada thread de plataforma é uma thread do sistema operacional: cara, com cerca de 1 MB de pilha e limitada a poucos milhares. ' +
    'Virtual threads (Java 21) são gerenciadas pela JVM. Quando bloqueiam (I/O, banco, sleep), liberam a thread do SO, ' +
    'e o código continua simples e bloqueante. No PetClinic basta <code>spring.threads.virtual.enabled=true</code>.',
    diagram('Por que fica mais rápido', THREADS_DIAGRAM) +
      '<h3 class="mt-3">No código</h3>' +
      source('src/main/resources/application.properties', 'props', 33, 'spring.threads.virtual.enabled=true',
        { 33: 'Tomcat e @Async passam a usar virtual threads' }) +
      source('dashboard/BenchmarkDemo.java', 'java', 57, `
	private static long measure(ExecutorService executor, int tasks) {
		long start = System.nanoTime();
		try (executor) {
			for (int i = 0; i < tasks; i++) {
				executor.submit(() -> {
					Thread.sleep(Duration.ofSeconds(1));
					return null;
				});
			}
		}
		return (System.nanoTime() - start) / 1_000_000;
	}`, { 59: 'fechar o executor = esperar todas', 62: 'bloqueia: a virtual thread desmonta' }),
    `<h3>Quem atendeu esta requisição?</h3>
     <div class="dash-actions"><button class="btn btn-primary btn-sm" id="thread">GET /thread</button></div>
     <div class="dash-result" id="thread-result"><span class="dash-empty">Mostra o nome e o tipo da thread do Tomcat.</span></div>
     <h3 class="mt-3">Benchmark no servidor</h3>
     <div class="dash-actions">
       <select class="form-select form-select-sm w-auto" id="task-count">
         <option value="500">500 tarefas</option>
         <option value="1000" selected>1.000 tarefas</option>
         <option value="2000">2.000 tarefas</option>
       </select>
       <button class="btn btn-primary btn-sm" id="benchmark">Rodar (cada tarefa: sleep 1 s)</button>
     </div>
     <div class="dash-result" id="benchmark-result"><span class="dash-empty">Pool fixo de 200 threads x uma virtual thread por tarefa.</span></div>`,
    ['demos/threads/BenchmarkThreads.java', 'application.properties', 'virtualthreads/ThreadInfoController.java']));

  // ---------- 3. Observer + @Async ----------
  cards.insertAdjacentHTML('beforeend', card('ext3', 3, 'Padrão Observer + processamento assíncrono', 'chamada direta e síncrona', 'evento + @Async',
    'O <code>VisitController</code> (o sujeito) só publica um <code>VisitRegisteredEvent</code> e não conhece quem vai reagir. ' +
    'O <code>VisitReminderListener</code> (o observador) recebe o evento em <b>outra thread</b> com <code>@Async</code>, ' +
    'então a resposta HTTP não espera os 2 s do "envio" do lembrete.',
    diagram('Registrar visita &rarr; evento &rarr; lembrete', OBSERVER_DIAGRAM) +
      '<h3 class="mt-3">O sujeito e o observador</h3>' +
      source('owner/VisitController.java', 'java', 115, `
		owner.addVisit(petId, visit);
		this.owners.save(owner);

		this.events.publishEvent(new VisitRegisteredEvent(this, owner.getFirstName() + " " + owner.getLastName(),
				owner.getPet(petId).getName(), visit.getDate(), visit.getDescription()));`,
        { 116: 'grava no banco', 118: 'só publica: não conhece o listener' }) +
      source('reminder/VisitReminderListener.java', 'java', 37, `
	@Async
	@EventListener
	public void sendReminder(VisitRegisteredEvent event) throws InterruptedException {
		Thread thread = Thread.currentThread();`,
        { 37: 'roda em outra thread', 38: 'recebe o evento pelo tipo' }),
    `<h3>Demo ao vivo: registra uma visita de verdade</h3>
     <div class="dash-actions">
       <select class="form-select form-select-sm" id="visit-pet"><option>carregando pets...</option></select>
       <input class="form-control form-control-sm" id="visit-description" value="Vacina anual" maxlength="255">
       <button class="btn btn-primary btn-sm" id="visit">Registrar visita</button>
     </div>
     <div class="dash-result" id="visit-result"><span class="dash-empty">Envia o mesmo POST do formulário. Repare no tempo da resposta e no horário do lembrete.</span></div>`,
    ['reminder/VisitRegisteredEvent.java', 'reminder/VisitReminderListener.java', 'reminder/AsyncConfiguration.java', 'owner/VisitController.java']));

  // ---------- 4. Strategy ----------
  cards.insertAdjacentHTML('beforeend', card('ext4', 4, 'Padrão Strategy: preço da consulta', 'if/switch por tipo', 'Strategy + injeção',
    'Cada regra de preço é uma classe que implementa <code>VisitPriceStrategy</code>, registrada com o <b>nome do tipo de pet</b>. ' +
    'O Spring injeta um <code>Map&lt;String, VisitPriceStrategy&gt;</code> com todas elas, e a escolha é ' +
    '<code>getOrDefault(type, defaultStrategy)</code>, sem nenhum <code>if</code>. Um tipo novo exige só uma classe nova.',
    diagram('Estrutura', STRATEGY_DIAGRAM) + `
      <h3 class="mt-3">A escolha, sem if/switch</h3>
      ${source('pricing/VisitPriceService.java', 'java', 37, `
	public VisitPriceService(Map<String, VisitPriceStrategy> strategiesByType) {
		this.strategiesByType = strategiesByType;
		this.defaultStrategy = strategiesByType.get(DefaultPriceStrategy.NAME);
	}

	public BigDecimal calculatePrice(Pet pet) {
		return strategyFor(pet).calculatePrice(pet);
	}

	public VisitPriceStrategy strategyFor(Pet pet) {
		String type = Optional.ofNullable(pet.getType()).map(PetType::getName).orElse(DefaultPriceStrategy.NAME);
		return this.strategiesByType.getOrDefault(type, this.defaultStrategy);
	}`, { 37: 'o Spring injeta todas as estratégias', 48: 'a escolha inteira' })}
      ${source('pricing/DogPriceStrategy.java', 'java', 26, `
@Component("dog")
class DogPriceStrategy implements VisitPriceStrategy {`, { 26: 'nome do bean = tipo do pet' })}
      <p class="small text-muted mt-2 mb-0">Cão: R$ 100 + R$ 5 por ano de idade &middot; Gato: R$ 90 &middot; Padrão: R$ 70</p>`,
    `<h3>Preço calculado para cada pet do banco</h3>
     <div class="dash-table-scroll" id="prices"><span class="dash-empty">carregando...</span></div>`,
    ['pricing/VisitPriceStrategy.java', 'pricing/DogPriceStrategy.java', 'pricing/CatPriceStrategy.java', 'pricing/DefaultPriceStrategy.java', 'pricing/VisitPriceService.java', 'pricing/VisitPriceWebConfiguration.java']));

  // ---------- 5. AOP ----------
  cards.insertAdjacentHTML('beforeend', card('ext5', 5, 'Aspectos: auditoria com @Audited', 'AspectJ (ajc)', 'Spring AOP (proxies)',
    'Log e medição de tempo são <i>cross-cutting concerns</i>: aparecem em todo lugar. Com AOP eles ficam num só lugar, o <code>AuditAspect</code>, ' +
    'e são "costurados" (weaving) nos métodos marcados com <code>@Audited</code>, sem editar o corpo deles. O Spring faz o weaving ' +
    'com proxies em tempo de execução, sem o compilador especial do AspectJ.',
    diagram('Como a chamada passa pelo aspecto', AOP_DIAGRAM) +
      '<h3 class="mt-3">O aspecto e onde ele entra</h3>' +
      source('audit/AuditAspect.java', 'java', 37, `
	@Around("@annotation(audited)")
	public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
		String method = joinPoint.getSignature().toShortString();
		String args = Arrays.toString(joinPoint.getArgs());
		long start = System.nanoTime();
		try {
			return joinPoint.proceed();
		}
		finally {
			long durationMs = (System.nanoTime() - start) / 1_000_000;
			log.info("[AUDITORIA] {} {} | args={} | {} ms | thread={}", method,`,
        { 37: 'pointcut: métodos com @Audited', 43: 'join point: o método original', 47: 'advice: sempre depois' }) +
      source('owner/VisitController.java', 'java', 103, '\t@Audited("Registrar visita")',
        { 103: 'a única mudança no controller' }) + `
      <h3 class="mt-3">Controllers no contexto do Spring</h3>
      <div id="aop"><span class="dash-empty">carregando...</span></div>`,
    `<h3>Demo ao vivo: chame métodos auditados</h3>
     <div class="dash-actions">
       <button class="btn btn-primary btn-sm" data-audit-url="/owners/1">GET /owners/1</button>
       <button class="btn btn-primary btn-sm" data-audit-url="/owners/6">GET /owners/6</button>
       <button class="btn btn-primary btn-sm" data-audit-url="/vets">GET /vets</button>
       <button class="btn btn-outline-secondary btn-sm" data-audit-url="/">GET / (não auditado)</button>
     </div>
     <div class="dash-result" id="aop-result"><span class="dash-empty">As linhas [AUDITORIA] geradas aparecem aqui e no console.</span></div>`,
    ['audit/Audited.java', 'audit/AuditAspect.java', 'OwnerController / VisitController / VetController (só a anotação)']));

  // ---------- 6. Reports ----------
  cards.insertAdjacentHTML('beforeend', card('ext6', 6, 'Relatórios', 'iReport', 'Jaspersoft Studio + JasperReports 7',
    'O JasperReports continua sendo o motor de relatórios mais usado no mundo Java. O que mudou foi o editor: o iReport foi ' +
    'substituído pelo Jaspersoft Studio, e a versão 7 trouxe um formato <code>.jrxml</code> mais simples. O layout fica separado ' +
    'dos dados: o relatório não sabe calcular preço, só recebe as linhas prontas do <code>VisitPriceService</code> (Strategy).',
    diagram('Compilar → preencher → exportar', REPORT_DIAGRAM) + `
      <h3 class="mt-3">As partes do .jrxml</h3>
      <pre class="dash-code"><span class="k">&lt;parameter&gt;</span> GENERATED_AT, DATABASE <span class="c">// vêm do Java</span>
<span class="k">&lt;field&gt;</span>     pet, type, price...    <span class="c">// getters de ReportRow</span>
<span class="k">&lt;variable&gt;</span>  TOTAL_PRICE = Sum      <span class="c">// calculada pelo Jasper</span>
<span class="k">&lt;group&gt;</span>     byType                 <span class="c">// cabeçalho e subtotal por tipo</span>
<span class="t">bandas</span>      title, columnHeader, detail, summary, pageFooter</pre>`,
    `<h3>Demo ao vivo: gerar o PDF com os dados do banco</h3>
     <div class="dash-actions">
       <button class="btn btn-primary btn-sm" id="report">Gerar relatório PDF</button>
       <a class="btn btn-outline-secondary btn-sm" href="/relatorios/pets-e-precos.pdf" target="_blank" rel="noopener">abrir em outra aba</a>
     </div>
     <div class="dash-result" id="report-result"><span class="dash-empty">O método do controller tem <code>@Audited</code>: o tempo de geração aparece no console.</span></div>`,
    ['src/main/resources/reports/pets-and-prices.jrxml', 'report/ReportService.java', 'report/ReportRow.java', 'report/ReportController.java']));

  // ---------- 7. Database ----------
  cards.insertAdjacentHTML('beforeend', card('ext7', 7, 'Banco de dados: do objeto à tabela', 'JDBC + SQL à mão', 'JPA/Hibernate + Spring Data',
    'O CRUD do PetClinic não tem SQL escrito à mão. As entidades são classes Java anotadas com JPA, o Hibernate converte objetos em linhas ' +
    '(ORM) e o Spring Data cria a implementação dos repositórios a partir do nome dos métodos. Em produção o mesmo código fala com o ' +
    'PostgreSQL do Neon; nos testes, com H2 em memória. Só muda a URL de conexão.',
    diagram('O caminho de uma consulta', DB_DIAGRAM) + `
      <h3 class="mt-3">A classe e a tabela</h3>
      <div class="dash-cols">
        ${source('owner/Pet.java', 'java', 44, `
@Entity
@Table(name = "pets")
public class Pet extends NamedEntity {

	@Column
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate birthDate;

	@ManyToOne
	@JoinColumn(name = "type_id")
	private PetType type;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "pet_id")
	@OrderBy("date ASC")
	private final Set<Visit> visits = new LinkedHashSet<>();`, { 45: '① tabela', 50: '② coluna birth_date', 53: '③ chave estrangeira', 57: '④ visits.pet_id' })}
        ${source('db/postgres/schema.sql', 'sql', 36, `
CREATE TABLE IF NOT EXISTS pets (
  id         INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  name       TEXT,
  birth_date DATE,
  type_id    INT NOT NULL REFERENCES types (id),
  owner_id   INT REFERENCES owners (id)
);`, { 36: '①', 39: '②', 40: '③' })}
      </div>
      ${source('db/postgres/schema.sql', 'sql', 47, `
CREATE TABLE IF NOT EXISTS visits (
  id          INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  pet_id      INT REFERENCES pets (id),`, { 49: '④' })}
      <h3 class="mt-3">Modelo entidade-relacionamento</h3>
      <div class="dash-diagram"><pre class="mermaid">${escapeHtml(ER_DIAGRAM)}</pre></div>`,
    `<h3>Demo ao vivo: o banco agora</h3>
     <div class="dash-actions"><button class="btn btn-primary btn-sm" id="db-count">Contar linhas de cada tabela</button></div>
     <div class="dash-result" id="db-count-result"><span class="dash-empty">Um SELECT COUNT(*) por tabela, direto no banco em uso.</span></div>
     <h3 class="mt-3">Demo ao vivo: o SQL que o Hibernate gera</h3>
     <div class="dash-actions">
       ${Object.entries(SQL_DEMOS).map(([id, d]) => '<button class="btn btn-outline-secondary btn-sm" data-sql-demo="' + id + '">' + d.label + '</button>').join('')}
     </div>
     <div class="dash-result" id="sql-result"><span class="dash-empty">Escolha uma chamada. A página mostra o método Java, cada comando SQL enviado ao banco e os objetos que voltaram.</span></div>`,
    ['owner/Pet.java', 'owner/Owner.java', 'owner/OwnerRepository.java', 'db/postgres/schema.sql', 'application-postgres.properties', 'dashboard/SqlRecorder.java']));

  // ---------- UML gallery (docs/uml/svg) ----------

  const UML_DIAGRAMS = [
    ['01_casos_de_uso', 'Casos de uso', 'requisitos'],
    ['02_classes_dominio', 'Classes do domínio', 'análise'],
    ['03_componentes_arquitetura', 'Componentes da arquitetura', 'arquitetura'],
    ['04_sequencia_registro_visita', 'Sequência: registro de visita', 'comportamento'],
    ['05_classes_strategy', 'Classes: Strategy', 'projeto'],
    ['06_classes_observer', 'Classes: Observer', 'projeto'],
    ['07_classes_aop', 'Classes: Spring AOP', 'projeto'],
    ['08_sequencia_grpc', 'Sequência: gRPC', 'comportamento'],
    ['09_implantacao_execucao_local', 'Implantação: execução local', 'implantação'],
    ['10_atividade_cadastro_pet', 'Atividade: cadastro de pet', 'comportamento'],
    ['11_implantacao_producao_render_neon', 'Implantação: Render + Neon', 'implantação']
  ];

  cards.insertAdjacentHTML('beforeend', `
    <section class="dash-card" id="uml">
      <h2><span class="dash-num">U</span> Documentação UML <span class="dash-tag">item 7.3 do plano</span></h2>
      <p class="dash-why">Onze diagramas cobrindo requisitos, análise, arquitetura, projeto, comportamento e implantação.
        Clique para abrir em tamanho real. As fontes PlantUML estão em <code>docs/uml/puml</code>.</p>
      <div class="dash-uml">
        ${UML_DIAGRAMS.map(([file, title, phase]) => `
          <a href="/uml/${file}.svg" target="_blank" rel="noopener">
            <img src="/uml/${file}.svg" alt="${title}" loading="lazy">
            <span><b>${file.slice(0, 2)}</b> ${title} <span class="dash-tag">${phase}</span></span>
          </a>`).join('')}
      </div>
    </section>`);

  // ---------------------------------------------------------------------------
  // Live console: polls new log lines every second
  // ---------------------------------------------------------------------------

  const consoleElement = document.getElementById('console');
  let lastLogId = 0;
  let consoleEmpty = true;
  const logListeners = [];

  function onLog(filter, callback) {
    const listener = { filter, callback };
    logListeners.push(listener);
    return () => logListeners.splice(logListeners.indexOf(listener), 1);
  }

  async function pollLogs() {
    try {
      const lines = await getJson('/api/dashboard/logs?after=' + lastLogId);
      for (const line of lines) {
        lastLogId = Math.max(lastLogId, line.id);
        if (consoleEmpty) {
          consoleElement.innerHTML = '';
          consoleEmpty = false;
        }
        consoleElement.insertAdjacentHTML('beforeend',
          '<div class="line"><span class="time">' + shortTime(line.time) + '</span> ' +
          '<span class="thread">[' + escapeHtml(line.thread) + ']</span> ' +
          '<span class="' + escapeHtml(line.source) + '">' + escapeHtml(line.message) + '</span></div>');
        logListeners.slice().forEach((l) => l.filter(line) && l.callback(line));
      }
      if (lines.length) {
        consoleElement.scrollTop = consoleElement.scrollHeight;
      }
    }
    catch (e) {
      // server restarting: retry on the next tick
    }
  }

  document.getElementById('clear-console').addEventListener('click', () => {
    consoleElement.innerHTML = '';
  });

  // Skip what was logged before the page opened
  getJson('/api/dashboard/logs?after=0').then((lines) => {
    lines.forEach((l) => { lastLogId = Math.max(lastLogId, l.id); });
  }).catch(() => {}).finally(() => setInterval(pollLogs, 1000));

  // ---------------------------------------------------------------------------
  // Runtime header
  // ---------------------------------------------------------------------------

  getJson('/api/dashboard/environment').then((env) => {
    const neon = /neon\.tech/.test(env.databaseUrl) ? ' (Neon)' : '';
    document.getElementById('runtime').innerHTML = [
      'Java <b>' + escapeHtml(env.java) + '</b>',
      'Spring Boot <b>' + escapeHtml(env.springBoot) + '</b>',
      'Banco <b>' + escapeHtml(env.database) + ' ' + escapeHtml(String(env.databaseVersion).split(' ')[0]) + neon + '</b>',
      'Virtual threads <b>' + (env.virtualThreads ? 'ligadas' : 'desligadas') + '</b>',
      'Profiles <b>' + escapeHtml(env.profiles.length ? env.profiles.join(', ') : 'default') + '</b>'
    ].map((t) => '<span class="dash-chip">' + t + '</span>').join('') +
      '<span class="dash-runtime-url">' + escapeHtml(env.databaseUrl) + '</span>';
  }).catch((e) => showError(document.getElementById('runtime'), e));

  // ---------------------------------------------------------------------------
  // 1. gRPC
  // ---------------------------------------------------------------------------

  document.getElementById('grpc').addEventListener('click', async () => {
    const target = document.getElementById('grpc-result');
    target.innerHTML = '<span class="dash-empty">chamando...</span>';
    try {
      const r = await getJson('/api/dashboard/grpc');
      const savings = Math.round(100 - (r.protobufBytes * 100) / r.jsonBytes);
      target.innerHTML = `
        <div class="dash-metrics">
          <div class="dash-metric"><span class="value">${r.durationMs} ms</span><span class="label">ida e volta até ${escapeHtml(r.target)}</span></div>
          <div class="dash-metric"><span class="value">${r.protobufBytes} B</span><span class="label">resposta em Protobuf</span></div>
          <div class="dash-metric"><span class="value">${r.jsonBytes} B</span><span class="label">mesmos dados em JSON</span></div>
          <div class="dash-metric"><span class="value">${savings}%</span><span class="label">menor em Protobuf</span></div>
        </div>
        <table class="dash-table"><tr><th>#</th><th>Veterinário</th><th>Especialidades</th></tr>
          ${r.vets.map((v) => `<tr><td>${v.id}</td><td>${escapeHtml(v.firstName)} ${escapeHtml(v.lastName)}</td>
            <td>${v.specialties.length ? v.specialties.map(escapeHtml).join(', ') : '<span class="dash-empty">nenhuma</span>'}</td></tr>`).join('')}
        </table>`;
    }
    catch (e) {
      showError(target, e);
    }
  });

  // ---------------------------------------------------------------------------
  // 2. Thread info and benchmark
  // ---------------------------------------------------------------------------

  document.getElementById('thread').addEventListener('click', async () => {
    const target = document.getElementById('thread-result');
    try {
      const t = await getJson('/thread');
      target.innerHTML = `
        <p class="mb-1">Nome: <code>${escapeHtml(t.name)}</code>
          <span class="dash-badge ${t.virtual ? 'dash-yes' : 'dash-no'}">${t.virtual ? 'virtual thread' : 'thread de plataforma'}</span></p>
        <pre class="dash-code">${escapeHtml(t.details)}</pre>
        <p class="small text-muted mt-1 mb-0">${t.virtual
          ? 'Depois da "/" aparece a <b>carrier thread</b>, a thread real do SO (ForkJoinPool) em que a virtual thread está montada agora. Clique de novo: a carrier pode mudar.'
          : 'As virtual threads estão desligadas nesta execução.'}</p>`;
    }
    catch (e) {
      showError(target, e);
    }
  });

  document.getElementById('benchmark').addEventListener('click', async (event) => {
    const button = event.currentTarget;
    const target = document.getElementById('benchmark-result');
    const tasks = Number(document.getElementById('task-count').value);
    const estimate = Math.ceil(tasks / 200) + 1;
    button.disabled = true;
    target.innerHTML = `<span class="dash-empty">rodando... cerca de ${estimate} s (${tasks} / 200 rodadas de 1 s no pool fixo, mais cerca de 1 s com virtual threads)</span>`;
    try {
      const response = await fetch('/api/dashboard/benchmark?tasks=' + tasks);
      if (response.status === 429) {
        throw new Error('Já existe um benchmark rodando. Aguarde alguns segundos.');
      }
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }
      const r = await response.json();
      const longest = Math.max(r.platformMs, r.virtualMs);
      const bar = (label, ms, color) => `
        <div class="dash-bar"><div class="label"><span>${label}</span><b>${ms.toLocaleString('pt-BR')} ms</b></div>
          <div class="track"><div class="fill" style="width:0;background:${color}" data-width="${Math.max(2, (ms * 100) / longest)}%"></div></div></div>`;
      target.innerHTML = bar(`Pool fixo (${r.poolSize} threads de plataforma)`, r.platformMs, '#d9822b') +
        bar('Virtual threads (uma por tarefa)', r.virtualMs, '#6db33f') +
        `<p class="small text-muted mt-1 mb-0">${r.tasks.toLocaleString('pt-BR')} tarefas: virtual threads foram
         <b>${(r.platformMs / r.virtualMs).toFixed(1)}x</b> mais rápidas, com o mesmo código de tarefa.</p>`;
      requestAnimationFrame(() => target.querySelectorAll('.fill').forEach((b) => { b.style.width = b.dataset.width; }));
    }
    catch (e) {
      showError(target, e);
    }
    finally {
      button.disabled = false;
    }
  });

  // ---------------------------------------------------------------------------
  // 3 and 4. Pets, prices and visit registration
  // ---------------------------------------------------------------------------

  getJson('/api/dashboard/pets').then((pets) => {
    document.getElementById('prices').innerHTML = `
      <table class="dash-table"><tr><th>Pet</th><th>Tipo</th><th>Idade</th><th>Estratégia escolhida</th><th>Preço</th><th></th></tr>
        ${pets.map((p) => `<tr>
          <td>${escapeHtml(p.pet)}<br><span class="small text-muted">${escapeHtml(p.owner)}</span></td>
          <td>${escapeHtml(p.type)}</td><td>${p.age}</td>
          <td><span class="dash-badge dash-strategy-${escapeHtml(p.strategy)}">${escapeHtml(p.strategy)}</span></td>
          <td><b>${currency(p.price)}</b></td>
          <td><a href="/owners/${p.ownerId}/pets/${p.petId}/visits/new?lang=pt" target="_blank" title="abrir a tela real de nova visita">tela</a></td>
        </tr>`).join('')}
      </table>`;
    document.getElementById('visit-pet').innerHTML = pets.map((p) =>
      `<option value="${p.ownerId}/${p.petId}">${escapeHtml(p.pet)} (${escapeHtml(p.type)}) de ${escapeHtml(p.owner)}</option>`).join('');
  }).catch((e) => showError(document.getElementById('prices'), e));

  document.getElementById('visit').addEventListener('click', async (event) => {
    const button = event.currentTarget;
    const target = document.getElementById('visit-result');
    const [ownerId, petId] = document.getElementById('visit-pet').value.split('/');
    const tomorrow = new Date(Date.now() + 86400000);
    const date = tomorrow.getFullYear() + '-' + String(tomorrow.getMonth() + 1).padStart(2, '0') + '-' + String(tomorrow.getDate()).padStart(2, '0');
    const form = new URLSearchParams({ date, description: document.getElementById('visit-description').value });

    button.disabled = true;
    target.innerHTML = '<span class="dash-empty">enviando...</span><ul class="dash-timeline" id="visit-timeline"></ul>';
    const timeline = document.getElementById('visit-timeline');

    // Follow the logs of this visit: the controller's audit line and the listener's lines
    let firstTime = null;
    const stopListening = onLog(
      (l) => /processNewVisitForm/.test(l.message) || l.source === 'VisitReminderListener',
      (l) => {
        const time = new Date(l.time).getTime();
        firstTime = firstTime ?? time;
        const isAudit = l.source === 'AuditAspect';
        timeline.insertAdjacentHTML('beforeend',
          `<li class="${isAudit ? 'audit' : ''}"><span class="delta">+${time - firstTime} ms</span>
           <code>${escapeHtml(l.thread)}</code> ${escapeHtml(truncate(l.message, 160))}</li>`);
        if (/lembrete enviado/.test(l.message)) {
          stopListening();
          target.insertAdjacentHTML('beforeend', '<p class="small text-muted mb-0">Repare: a resposta HTTP voltou antes do lembrete terminar. A thread do lembrete (<code>task-N</code>) é diferente da thread da requisição (<code>tomcat-handler-N</code>).</p>');
        }
      });

    const start = performance.now();
    try {
      // Same POST as the PetClinic form; redirect: 'manual' so only this request is timed
      const response = await fetch(`/owners/${ownerId}/pets/${petId}/visits/new`, {
        method: 'POST', body: form, redirect: 'manual'
      });
      const ms = Math.round(performance.now() - start);
      const saved = response.type === 'opaqueredirect' || response.redirected || (response.status >= 300 && response.status < 400);
      target.querySelector('.dash-empty').outerHTML = saved
        ? `<div class="dash-metrics"><div class="dash-metric"><span class="value">${ms} ms</span><span class="label">resposta HTTP (302, visita salva)</span></div>
             <div class="dash-metric"><span class="value">2.000 ms</span><span class="label">tempo do "envio" do lembrete</span></div></div>`
        : '<div class="alert alert-warning py-2">O formulário recusou os dados (validação). Tente outra descrição.</div>';
      if (!saved) {
        stopListening();
      }
    }
    catch (e) {
      stopListening();
      showError(target, e);
    }
    finally {
      button.disabled = false;
    }
  });

  // ---------------------------------------------------------------------------
  // 5. AOP
  // ---------------------------------------------------------------------------

  getJson('/api/dashboard/aop').then((controllers) => {
    document.getElementById('aop').innerHTML = `
      <table class="dash-table"><tr><th>Bean</th><th>Classe em tempo de execução</th><th>Métodos @Audited</th></tr>
        ${controllers.map((c) => `<tr>
          <td>${escapeHtml(c.bean)}</td>
          <td><code>${escapeHtml(c.runtimeClass)}</code><br>
            <span class="dash-badge ${c.proxy ? 'dash-yes' : 'dash-no'}">${c.proxy ? 'proxy AOP' : 'objeto comum'}</span></td>
          <td>${c.auditedMethods.length ? c.auditedMethods.map((m) => '<code>' + escapeHtml(m) + '</code>').join('<br>') : '<span class="dash-empty">nenhum</span>'}</td>
        </tr>`).join('')}
      </table>`;
  }).catch((e) => showError(document.getElementById('aop'), e));

  document.querySelectorAll('[data-audit-url]').forEach((button) => {
    button.addEventListener('click', async () => {
      const url = button.dataset.auditUrl;
      const target = document.getElementById('aop-result');
      target.innerHTML = '<ul class="dash-timeline" id="aop-lines"></ul>';
      const list = document.getElementById('aop-lines');
      let received = false;
      const stop = onLog((l) => l.source === 'AuditAspect', (l) => {
        received = true;
        list.insertAdjacentHTML('beforeend', `<li class="audit">${escapeHtml(truncate(l.message, 220))}</li>`);
      });
      try {
        const response = await fetch(url);
        list.insertAdjacentHTML('beforebegin', `<p class="small mb-1">GET ${escapeHtml(url)} &rarr; HTTP ${response.status}</p>`);
      }
      catch (e) {
        showError(target, e);
      }
      // wait for the next console poll (1 s) to show what the aspect logged
      setTimeout(() => {
        stop();
        if (!received) {
          list.insertAdjacentHTML('beforeend', '<li>Nenhuma linha [AUDITORIA]: este método não tem @Audited, então o aspecto não o intercepta.</li>');
        }
      }, 2500);
    });
  });

  // ---------------------------------------------------------------------------
  // 6. PDF report
  // ---------------------------------------------------------------------------

  document.getElementById('report').addEventListener('click', async (event) => {
    const button = event.currentTarget;
    const target = document.getElementById('report-result');
    button.disabled = true;
    target.innerHTML = '<span class="dash-empty">gerando...</span>';
    const start = performance.now();
    try {
      const response = await fetch('/relatorios/pets-e-precos.pdf');
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }
      const pdf = await response.blob();
      const ms = Math.round(performance.now() - start);
      const url = URL.createObjectURL(pdf);
      target.innerHTML = `
        <div class="dash-metrics">
          <div class="dash-metric"><span class="value">${ms} ms</span><span class="label">preencher + exportar</span></div>
          <div class="dash-metric"><span class="value">${(pdf.size / 1024).toFixed(0)} KB</span><span class="label">PDF gerado</span></div>
        </div>
        <iframe class="dash-pdf" src="${url}" title="Relatório PDF"></iframe>`;
    }
    catch (e) {
      showError(target, e);
    }
    finally {
      button.disabled = false;
    }
  });

  // ---------------------------------------------------------------------------
  // Topic 1: generics
  // ---------------------------------------------------------------------------

  document.getElementById('generics').addEventListener('click', async () => {
    const target = document.getElementById('generics-result');
    try {
      const g = await getJson('/api/dashboard/generics');
      target.innerHTML = `
        <p class="mb-1"><b>1. Type erasure:</b> <code>new ArrayList&lt;String&gt;().getClass() == new ArrayList&lt;Integer&gt;().getClass()</code>
          &rarr; <span class="dash-badge ${g.sameClass ? 'dash-yes' : 'dash-no'}">${g.sameClass}</span>
          <span class="small text-muted">(as duas são <code>${escapeHtml(g.stringListClass)}</code>; o compilador apaga o tipo)</span></p>
        <p class="mb-1"><b>2. Mas o tipo fica nas assinaturas.</b> O Spring Data lê o genérico de cada interface:</p>
        <pre class="dash-code">${g.repositories.map(escapeHtml).join('\n')}</pre>
        <p class="mb-1 mt-2"><b>3. E injeta por tipo genérico:</b> <code>${escapeHtml(g.genericInjection)}</code>
          recebeu <b>${g.injectedStrategies}</b> beans, porque só eles combinam com o tipo do valor do Map.</p>
        <p class="mb-0"><b>4. Proxy gerado:</b> <code>VetRepository</code> não tem classe de implementação. Em tempo de execução ele é
          <code>${escapeHtml(g.vetRepositoryRuntimeClass)}</code>, criado a partir de <code>Repository&lt;Vet, Integer&gt;</code>.</p>`;
    }
    catch (e) {
      showError(target, e);
    }
  });

  // ---------------------------------------------------------------------------
  // 7. Database
  // ---------------------------------------------------------------------------

  document.getElementById('db-count').addEventListener('click', async () => {
    const target = document.getElementById('db-count-result');
    target.innerHTML = '<span class="dash-empty">contando...</span>';
    try {
      const counts = await getJson('/api/dashboard/database');
      const max = Math.max(...counts.map((c) => c.rows), 1);
      target.innerHTML = counts.map((c) => `
        <div class="dash-bar"><div class="label"><span><code>${escapeHtml(c.table)}</code></span><b>${c.rows.toLocaleString('pt-BR')}</b></div>
          <div class="track"><div class="fill" style="width:${Math.max(2, (c.rows * 100) / max)}%;background:var(--dash-verde)"></div></div></div>`).join('');
    }
    catch (e) {
      showError(target, e);
    }
  });

  root.querySelectorAll('[data-sql-demo]').forEach((button) => button.addEventListener('click', async () => {
    const demo = SQL_DEMOS[button.dataset.sqlDemo];
    const target = document.getElementById('sql-result');
    target.innerHTML = '<span class="dash-empty">executando...</span>';
    try {
      const r = await getJson('/api/dashboard/sql?demo=' + encodeURIComponent(button.dataset.sqlDemo));
      target.innerHTML = `
        <ol class="dash-steps">
          <li><h4>Chamada Java</h4>${demo.code()}<p class="small mb-0">${demo.why}</p></li>
          <li><h4>SQL enviado ao banco: ${r.sql.length} ${r.sql.length === 1 ? 'comando' : 'comandos'} em ${r.millis} ms</h4>
            ${r.sql.map((q, i) => source('comando ' + (i + 1), 'sql', 1, formatSql(q))).join('')}</li>
          <li><h4>Objetos devolvidos: ${r.objects}</h4>
            <pre class="dash-objects">${r.result.map(escapeHtml).join('\n') || '(nenhum)'}</pre></li>
        </ol>`;
    }
    catch (e) {
      showError(target, e);
    }
  }));

  // ---------------------------------------------------------------------------
  // Diagrams: render with the page font, after it loads, or Mermaid clips the labels
  // ---------------------------------------------------------------------------

  if (window.mermaid) {
    window.mermaid.initialize({
      startOnLoad: false,
      theme: 'neutral',
      securityLevel: 'strict',
      fontFamily: '"varela_roundregular", sans-serif'
    });
    document.fonts.ready.then(() => window.mermaid.run({ querySelector: '#dashboard pre.mermaid' }));
  }
})();
