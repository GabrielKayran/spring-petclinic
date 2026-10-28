/*
 * Painel do estudo de caso (/painel).
 *
 * Monta a página e chama:
 *  - a API do painel (/api/painel/*), criada só para a apresentação;
 *  - os endpoints REAIS do PetClinic (/thread, formulário de visita, /owners/1, /vets),
 *    para que os aspectos, eventos e virtual threads atuem de verdade.
 *
 * JavaScript puro, sem framework, para ficar fácil de ler na aula.
 */
(function () {
  'use strict';

  const raiz = document.getElementById('painel');

  // ---------------------------------------------------------------------------
  // Utilitários
  // ---------------------------------------------------------------------------

  /** Escapa texto vindo do servidor antes de inserir como HTML. */
  function esc(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, (c) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  }

  async function getJson(url) {
    const resposta = await fetch(url, { headers: { Accept: 'application/json' } });
    if (!resposta.ok) {
      throw new Error('HTTP ' + resposta.status + ' em ' + url);
    }
    return resposta.json();
  }

  function moeda(valor) {
    return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  function horaCurta(instante) {
    const d = new Date(instante);
    return d.toLocaleTimeString('pt-BR', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0');
  }

  function erro(el, e) {
    el.innerHTML = '<div class="alert alert-danger py-2 mb-0">' + esc(e.message || e) + '</div>';
  }

  function arquivos(lista) {
    return '<p class="pn-arquivos"><b>No código:</b> ' + lista.map((a) => '<code>' + esc(a) + '</code>').join(' ') + '</p>';
  }

  function cartao(id, numero, titulo, classico, moderno, porque, esquerda, direita, listaArquivos) {
    return `
      <section class="pn-card" id="${id}">
        <h2><span class="pn-num">${numero}</span> ${titulo}
          <span class="pn-tag"><span class="pn-classico">${classico}</span> &rarr; <b>${moderno}</b></span>
        </h2>
        <p class="pn-porque">${porque}</p>
        <div class="pn-corpo">
          <div class="pn-box">${esquerda}</div>
          <div class="pn-box pn-demo">${direita}</div>
        </div>
        ${arquivos(listaArquivos)}
      </section>`;
  }

  function diagrama(titulo, codigoMermaid) {
    return '<h3>' + titulo + '</h3><div class="pn-diagrama"><pre class="mermaid">' + esc(codigoMermaid) + '</pre></div>';
  }

  // ---------------------------------------------------------------------------
  // Diagramas (Mermaid). As versões completas estão no ESTUDO-DE-CASO.md
  // ---------------------------------------------------------------------------

  // TB empilha os blocos; "direction LR" deixa o fluxo de cada bloco na horizontal;
  // o link invisível (~~~) mantém o clássico em cima e o moderno embaixo.
  const DIAGRAMA_GRPC = `flowchart TB
  subgraph C["RMI (clássico)"]
    direction LR
    CR["Cliente Java"] -- "serialização Java<br/>porta 1099 + dinâmicas" --> SR["Servidor Java"]
  end
  subgraph M["gRPC (moderno)"]
    direction LR
    CG["Cliente em qualquer<br/>linguagem (stub do .proto)"] -- "Protobuf / HTTP/2<br/>porta 9090" --> SG["VeterinarioGrpcService"]
    SG --> RG[("VetRepository<br/>PostgreSQL")]
  end
  C ~~~ M`;

  const DIAGRAMA_THREADS = `flowchart TB
  subgraph P["Pool fixo: 200 threads do SO"]
    direction LR
    T1["10.000 tarefas"] --> F["fila de espera"] --> W["200 threads<br/>bloqueadas no sleep"]
  end
  subgraph V["Virtual threads"]
    direction LR
    T2["10.000 virtual threads"] -- "bloqueou? é desmontada" --> CT["poucas carrier threads<br/>(ForkJoinPool)"]
  end
  P ~~~ V`;

  const DIAGRAMA_OBSERVER = `sequenceDiagram
  autonumber
  actor U as Navegador
  participant VC as VisitController
  participant P as ApplicationEventPublisher
  participant L as LembreteVisitaListener
  U->>VC: POST visita (virtual thread tomcat-handler)
  VC->>VC: owners.save(owner)
  VC->>P: publishEvent(VisitaRegistradaEvent)
  P-->>L: @Async: entrega em outra thread (task-N)
  VC-->>U: 302 Redirect (não espera o lembrete)
  Note over L: sleep 2 s (simula e-mail/SMS)
  L->>L: log "lembrete enviado"`;

  const DIAGRAMA_STRATEGY = `classDiagram
  direction LR
  class PrecoConsultaStrategy {
    <<interface>>
    +calcularPreco(Pet) BigDecimal
  }
  class PrecoConsultaService {
    -Map~String,Strategy~ estrategiasPorTipo
    +calcularPreco(Pet) BigDecimal
  }
  PrecoConsultaStrategy <|.. PrecoConsultaCao : bean "dog"
  PrecoConsultaStrategy <|.. PrecoConsultaGato : bean "cat"
  PrecoConsultaStrategy <|.. PrecoConsultaPadrao : bean "padrao"
  PrecoConsultaService o-- PrecoConsultaStrategy : Spring injeta todas`;

  const DIAGRAMA_AOP = `flowchart LR
  R["Requisição HTTP"] --> PX["Proxy CGLIB<br/>VetController$$SpringCGLIB"]
  PX --> A["AuditoriaAspect<br/>@Around('@annotation(auditado)')"]
  A -- "proceed()" --> M["VetController.showResourcesVetList()<br/>(código original)"]
  M -- "retorno" --> A
  A -- "log: método, args, ms" --> LOG[("log")]`;

  // ---------------------------------------------------------------------------
  // Estrutura da página
  // ---------------------------------------------------------------------------

  raiz.innerHTML = `
    <div class="pn-hero">
      <h1>Estudo de caso: Spring PetClinic</h1>
      <p>POO2 (UFU): cinco extensões, cada uma comparando a tecnologia clássica da disciplina com o seu equivalente moderno.
         Tudo nesta página roda <b>ao vivo</b> sobre a aplicação real.</p>
      <div id="pn-ambiente"><span class="pn-chip">carregando ambiente...</span></div>
    </div>

    <nav class="pn-nav">
      <a href="#mapa">Mapa da disciplina</a>
      <a href="#ext1">1. RMI &rarr; gRPC</a>
      <a href="#ext2">2. Virtual Threads</a>
      <a href="#ext3">3. Observer + @Async</a>
      <a href="#ext4">4. Strategy</a>
      <a href="#ext5">5. AOP @Auditado</a>
      <a href="#uml">UML</a>
    </nav>

    <div class="pn-grid">
      <div id="pn-cartoes"></div>
      <aside class="pn-console">
        <header>
          <span class="pn-ao-vivo">Console ao vivo: logs das extensões</span>
          <button type="button" id="pn-limpar">limpar</button>
        </header>
        <div class="linhas" id="pn-console"><div class="pn-vazio">Clique nos botões ao lado. Os logs do aspecto, do listener e do gRPC aparecem aqui.</div></div>
      </aside>
    </div>`;

  const cartoes = document.getElementById('pn-cartoes');

  // ---------- Mapa da disciplina: plano de ensino x código ----------

  /** Linhas de tabela: cada item é uma lista de células (HTML já escapado). */
  function tabela(cabecalho, linhas) {
    return '<table class="pn-tabela"><tr>' + cabecalho.map((c) => '<th>' + c + '</th>').join('') + '</tr>' +
      linhas.map((l) => '<tr>' + l.map((c) => '<td>' + c + '</td>').join('') + '</tr>').join('') + '</table>';
  }

  const TOPICOS = [
    ['1. Programação genérica', '<code>Repository&lt;Vet, Integer&gt;</code>, <code>Map&lt;String, PrecoConsultaStrategy&gt;</code>, <code>StreamObserver&lt;T&gt;</code>', 'demo logo abaixo'],
    ['2. Multithreading', 'virtual threads, <code>ExecutorService</code>, tarefas <code>Callable</code>/<code>Future</code>, <code>Semaphore</code> (sincronização), <code>@Async</code>', '<a href="#ext2">card 2</a>, <a href="#ext3">card 3</a>'],
    ['3. RMI', '<code>demos/rmi</code>: interface <code>Remote</code>, stub, Registry :1099 &rarr; sucessor gRPC', '<a href="#ext1">card 1</a>'],
    ['4. Frameworks (MVC, dados, testes)', 'Spring MVC (sucessor do Struts), JPA/Hibernate, JUnit 5 (99 testes). Relatórios (Jasper) não fazem parte do projeto', 'todo o sistema'],
    ['5. Princípios e padrões', 'Strategy, Observer, Proxy, Template Method, Builder, MVC, Broker... e SOLID', '<a href="#ext3">3</a>, <a href="#ext4">4</a>, <a href="#ext5">5</a> e tabela abaixo'],
    ['6. Aspectos', 'concern, pointcut, advice, join point, weaving: <code>@Auditado</code> + <code>AuditoriaAspect</code>', '<a href="#ext5">card 5</a>'],
    ['7.1 Aplicação integrada', 'as 5 extensões rodando juntas no mesmo PetClinic', 'esta página'],
    ['7.2 Persistência', 'PostgreSQL no Neon via JPA/Hibernate (veja o cabeçalho)', 'cabeçalho'],
    ['7.3 UML', '10 diagramas: casos de uso, classes, componentes, sequência, implantação, atividade', '<a href="#uml">galeria UML</a>']
  ];

  const PADROES = [
    ['Strategy', 'GoF comportamental', '<code>preco/PrecoConsulta*</code>', 'algoritmo de preço trocável, escolhido por um Map injetado'],
    ['Observer', 'GoF comportamental', '<code>lembrete/*</code> + <code>ApplicationEventPublisher</code>', 'o sujeito publica; os observadores reagem sem serem conhecidos'],
    ['Template Method', 'GoF comportamental', '<code>PainelLogAppender extends AppenderBase</code>', '<code>doAppend()</code> é o esqueleto fixo; nós só preenchemos <code>append()</code>'],
    ['Command', 'GoF comportamental', 'tarefas enviadas ao <code>ExecutorService</code> (<code>BenchmarkDemo</code>)', 'a ação vira objeto e roda depois, em outra thread'],
    ['Proxy', 'GoF estrutural', 'stubs RMI/gRPC, proxies CGLIB do AOP, <code>VetRepository</code> sem implementação', 'um substituto controla o acesso ao objeto real'],
    ['Adapter', 'GoF estrutural', '<code>VeterinarioGrpcService.paraMensagem()</code>', 'converte a entidade JPA <code>Vet</code> na mensagem Protobuf'],
    ['Builder', 'GoF criacional', '<code>Veterinario.newBuilder()...build()</code>, <code>ManagedChannelBuilder</code>', 'monta objetos imutáveis passo a passo'],
    ['Factory Method', 'GoF criacional', '<code>Executors.newVirtualThreadPerTaskExecutor()</code>, <code>newBlockingStub()</code>', 'quem chama não sabe qual classe concreta é criada'],
    ['Singleton', 'GoF criacional', 'beans do Spring (escopo padrão)', 'uma instância por contexto, garantida pelo container'],
    ['MVC / Layers', 'arquitetural', 'controllers &rarr; repositories &rarr; banco; views Thymeleaf', 'camadas com dependência em um só sentido'],
    ['Broker', 'arquitetural', 'RMI Registry / gRPC', 'cliente e servidor se comunicam por um intermediário de rede']
  ];

  const SOLID = [
    ['SRP', '<code>LembreteVisitaListener</code> só envia lembrete; <code>AuditoriaAspect</code> só audita'],
    ['OCP', 'tipo de pet novo = classe nova <code>@Component("hamster")</code>, sem editar nada'],
    ['LSP', 'qualquer <code>PrecoConsultaStrategy</code> substitui outra no serviço'],
    ['ISP', '<code>VetRepository</code> estende o marcador <code>Repository</code> e expõe só <code>findAll</code>, não os ~20 métodos de <code>JpaRepository</code>'],
    ['DIP', '<code>VisitController</code> depende de <code>ApplicationEventPublisher</code> (abstração), nunca do listener'],
    ['ADP (pacotes)', 'sem ciclos: <code>painel</code> &rarr; extensões &rarr; núcleo; nenhuma extensão depende do painel']
  ];

  cartoes.insertAdjacentHTML('beforeend', `
    <section class="pn-card" id="mapa">
      <h2><span class="pn-num">0</span> Mapa da disciplina <span class="pn-tag">plano de ensino &rarr; código</span></h2>
      <p class="pn-porque">O tópico 7 pede uma aplicação que use <b>princípios e padrões, multithreading, RMI e frameworks</b> (7.1),
        com <b>persistência em banco</b> (7.2) e <b>documentada em UML</b> (7.3). Cada linha aponta onde isso aparece neste sistema.</p>
      <div class="pn-corpo">
        <div class="pn-box"><h3>Tópicos do plano de ensino</h3>${tabela(['Tópico', 'Onde aparece', 'Ver'], TOPICOS)}</div>
        <div class="pn-box pn-demo">
          <h3>Demo ao vivo: genéricos e <i>type erasure</i></h3>
          <div class="pn-acoes"><button class="btn btn-primary btn-sm" id="pn-genericos">Inspecionar genéricos em tempo de execução</button></div>
          <div class="pn-resultado" id="pn-genericos-resultado"><span class="pn-vazio">O tipo genérico some dos objetos, mas continua nas assinaturas, e o Spring o lê para injetar dependências.</span></div>
        </div>
        <div class="pn-box"><h3>Padrões de projeto e arquiteturais no código</h3>${tabela(['Padrão', 'Categoria', 'Onde', 'O que mostrar'], PADROES)}</div>
        <div class="pn-box"><h3>Princípios SOLID e de pacotes</h3>${tabela(['Princípio', 'Exemplo no código'], SOLID)}
          <p class="small text-muted mt-2 mb-0"><b>Anti-padrões evitados:</b> cadeia de <code>if/switch</code> por tipo (&rarr; Strategy) e log espalhado em todos os métodos, o <i>scattering/tangling</i> (&rarr; AOP).</p>
        </div>
      </div>
    </section>`);

  // ---------- Extensão 1 ----------
  cartoes.insertAdjacentHTML('beforeend', cartao('ext1', 1, 'Chamada remota de métodos', 'RMI', 'gRPC',
    'O RMI só conversa Java com Java, usa serialização Java (histórico de falhas graves de segurança) e portas que firewalls bloqueiam. ' +
    'O gRPC mantém a ideia de "chamar um método remoto por um stub", mas com contrato <code>.proto</code> neutro de linguagem, ' +
    'mensagens binárias Protobuf e transporte HTTP/2.',
    diagrama('Antes e depois', DIAGRAMA_GRPC) + `
      <h3 class="mt-3">O contrato</h3>
      <div class="pn-cols">
        <pre class="pn-codigo"><span class="c">// RMI: interface Java</span>
<span class="k">interface</span> <span class="t">VeterinarioService</span>
    <span class="k">extends</span> Remote {
  List&lt;String&gt; listarVeterinarios()
      <span class="k">throws</span> RemoteException;
}</pre>
        <pre class="pn-codigo"><span class="c">// gRPC: veterinarios.proto</span>
<span class="k">service</span> <span class="t">VeterinarioService</span> {
  <span class="k">rpc</span> ListarVeterinarios
    (ListarVeterinariosRequest)
    <span class="k">returns</span> (ListarVeterinariosResponse);
}</pre>
      </div>`,
    `<h3>Demo ao vivo</h3>
     <div class="pn-acoes">
       <button class="btn btn-primary btn-sm" id="pn-grpc">Chamar ListarVeterinarios via gRPC</button>
     </div>
     <div class="pn-resultado" id="pn-grpc-resultado"><span class="pn-vazio">O servidor abre um canal HTTP/2 até a porta gRPC e chama o stub gerado.</span></div>
     <p class="small text-muted mt-2 mb-0">A demo de RMI puro roda só localmente: <code>cd demos/rmi</code>, <code>javac *.java</code>, <code>java Servidor</code> e, em outro terminal, <code>java Cliente</code>.</p>`,
    ['demos/rmi/*.java', 'src/main/protobuf/veterinarios.proto', 'grpc/VeterinarioGrpcService.java', 'grpc/cliente/ClienteGrpc.java']));

  // ---------- Extensão 2 ----------
  cartoes.insertAdjacentHTML('beforeend', cartao('ext2', 2, 'Concorrência', 'Threads de plataforma', 'Virtual Threads',
    'Cada thread de plataforma é uma thread do sistema operacional: cara, com cerca de 1 MB de pilha e limitada a poucos milhares. ' +
    'Virtual threads (Java 21) são gerenciadas pela JVM. Quando bloqueiam (I/O, banco, sleep), liberam a thread do SO, ' +
    'e o código continua simples e bloqueante. No PetClinic basta <code>spring.threads.virtual.enabled=true</code>.',
    diagrama('Por que fica mais rápido', DIAGRAMA_THREADS),
    `<h3>Quem atendeu esta requisição?</h3>
     <div class="pn-acoes"><button class="btn btn-primary btn-sm" id="pn-thread">GET /thread</button></div>
     <div class="pn-resultado" id="pn-thread-resultado"><span class="pn-vazio">Mostra o nome e o tipo da thread do Tomcat.</span></div>
     <h3 class="mt-3">Benchmark no servidor</h3>
     <div class="pn-acoes">
       <select class="form-select form-select-sm w-auto" id="pn-tarefas">
         <option value="500">500 tarefas</option>
         <option value="1000" selected>1.000 tarefas</option>
         <option value="2000">2.000 tarefas</option>
       </select>
       <button class="btn btn-primary btn-sm" id="pn-benchmark">Rodar (cada tarefa: sleep 1 s)</button>
     </div>
     <div class="pn-resultado" id="pn-benchmark-resultado"><span class="pn-vazio">Pool fixo de 200 threads x uma virtual thread por tarefa.</span></div>`,
    ['demos/threads/BenchmarkThreads.java', 'application.properties', 'virtualthreads/ThreadInfoController.java']));

  // ---------- Extensão 3 ----------
  cartoes.insertAdjacentHTML('beforeend', cartao('ext3', 3, 'Padrão Observer + processamento assíncrono', 'chamada direta e síncrona', 'evento + @Async',
    'O <code>VisitController</code> (o sujeito) só publica um <code>VisitaRegistradaEvent</code> e não conhece quem vai reagir. ' +
    'O <code>LembreteVisitaListener</code> (o observador) recebe o evento em <b>outra thread</b> com <code>@Async</code>, ' +
    'então a resposta HTTP não espera os 2 s do "envio" do lembrete.',
    diagrama('Registrar visita &rarr; evento &rarr; lembrete', DIAGRAMA_OBSERVER),
    `<h3>Demo ao vivo: registra uma visita de verdade</h3>
     <div class="pn-acoes">
       <select class="form-select form-select-sm" id="pn-visita-pet"><option>carregando pets...</option></select>
       <input class="form-control form-control-sm" id="pn-visita-descricao" value="Vacina anual" maxlength="255">
       <button class="btn btn-primary btn-sm" id="pn-visita">Registrar visita</button>
     </div>
     <div class="pn-resultado" id="pn-visita-resultado"><span class="pn-vazio">Envia o mesmo POST do formulário. Repare no tempo da resposta e no horário do lembrete.</span></div>`,
    ['lembrete/VisitaRegistradaEvent.java', 'lembrete/LembreteVisitaListener.java', 'lembrete/LembreteAsyncConfiguration.java', 'owner/VisitController.java']));

  // ---------- Extensão 4 ----------
  cartoes.insertAdjacentHTML('beforeend', cartao('ext4', 4, 'Padrão Strategy: preço da consulta', 'if/switch por tipo', 'Strategy + injeção',
    'Cada regra de preço é uma classe que implementa <code>PrecoConsultaStrategy</code>, registrada com o <b>nome do tipo de pet</b>. ' +
    'O Spring injeta um <code>Map&lt;String, PrecoConsultaStrategy&gt;</code> com todas elas, e a escolha é ' +
    '<code>getOrDefault(tipo, padrao)</code>, sem nenhum <code>if</code>. Um tipo novo exige só uma classe nova.',
    diagrama('Estrutura', DIAGRAMA_STRATEGY) + `
      <h3 class="mt-3">A escolha, sem if/switch</h3>
      <pre class="pn-codigo"><span class="c">// PrecoConsultaService</span>
<span class="k">return</span> estrategiasPorTipo
    .getOrDefault(tipo, estrategiaPadrao)
    .calcularPreco(pet);</pre>
      <p class="small text-muted mt-2 mb-0">Cão: R$ 100 + R$ 5 por ano de idade &middot; Gato: R$ 90 &middot; Padrão: R$ 70</p>`,
    `<h3>Preço calculado para cada pet do banco</h3>
     <div class="pn-tabela-rolagem" id="pn-precos"><span class="pn-vazio">carregando...</span></div>`,
    ['preco/PrecoConsultaStrategy.java', 'preco/PrecoConsultaCao.java', 'preco/PrecoConsultaGato.java', 'preco/PrecoConsultaPadrao.java', 'preco/PrecoConsultaService.java', 'preco/PrecoConsultaWebConfiguration.java']));

  // ---------- Extensão 5 ----------
  cartoes.insertAdjacentHTML('beforeend', cartao('ext5', 5, 'Aspectos: auditoria com @Auditado', 'AspectJ (ajc)', 'Spring AOP (proxies)',
    'Log e medição de tempo são <i>cross-cutting concerns</i>: aparecem em todo lugar. Com AOP eles ficam num só lugar, o <code>AuditoriaAspect</code>, ' +
    'e são "costurados" (weaving) nos métodos marcados com <code>@Auditado</code>, sem editar o corpo deles. O Spring faz o weaving ' +
    'com proxies em tempo de execução, sem o compilador especial do AspectJ.',
    diagrama('Como a chamada passa pelo aspecto', DIAGRAMA_AOP) + `
      <h3 class="mt-3">Controllers no contexto do Spring</h3>
      <div id="pn-aop"><span class="pn-vazio">carregando...</span></div>`,
    `<h3>Demo ao vivo: chame métodos auditados</h3>
     <div class="pn-acoes">
       <button class="btn btn-primary btn-sm" data-auditar="/owners/1">GET /owners/1</button>
       <button class="btn btn-primary btn-sm" data-auditar="/owners/6">GET /owners/6</button>
       <button class="btn btn-primary btn-sm" data-auditar="/vets">GET /vets</button>
       <button class="btn btn-outline-secondary btn-sm" data-auditar="/">GET / (não auditado)</button>
     </div>
     <div class="pn-resultado" id="pn-aop-resultado"><span class="pn-vazio">As linhas [AUDITORIA] geradas aparecem aqui e no console.</span></div>`,
    ['auditoria/Auditado.java', 'auditoria/AuditoriaAspect.java', 'OwnerController / VisitController / VetController (só a anotação)']));

  // ---------- Galeria UML (item 7.3), com os diagramas de docs/uml/svg ----------

  const DIAGRAMAS_UML = [
    ['01_casos_de_uso', 'Casos de uso', 'requisitos'],
    ['02_classes_dominio', 'Classes do domínio', 'análise'],
    ['03_componentes_arquitetura', 'Componentes da arquitetura', 'arquitetura'],
    ['04_sequencia_registro_visita', 'Sequência: registro de visita', 'comportamento'],
    ['05_classes_strategy', 'Classes: Strategy', 'projeto'],
    ['06_classes_observer', 'Classes: Observer', 'projeto'],
    ['07_classes_aop', 'Classes: Spring AOP', 'projeto'],
    ['08_sequencia_grpc', 'Sequência: gRPC', 'comportamento'],
    ['09_implantacao_execucao_local', 'Implantação', 'implantação'],
    ['10_atividade_cadastro_pet', 'Atividade: cadastro de pet', 'comportamento']
  ];

  cartoes.insertAdjacentHTML('beforeend', `
    <section class="pn-card" id="uml">
      <h2><span class="pn-num">U</span> Documentação UML <span class="pn-tag">item 7.3 do plano</span></h2>
      <p class="pn-porque">Dez diagramas cobrindo requisitos, análise, arquitetura, projeto, comportamento e implantação.
        Clique para abrir em tamanho real. As fontes PlantUML estão em <code>docs/uml/puml</code>.</p>
      <div class="pn-uml">
        ${DIAGRAMAS_UML.map(([arquivo, titulo, fase]) => `
          <a href="/uml/${arquivo}.svg" target="_blank" rel="noopener">
            <img src="/uml/${arquivo}.svg" alt="${titulo}" loading="lazy">
            <span><b>${arquivo.slice(0, 2)}</b> ${titulo} <span class="pn-tag">${fase}</span></span>
          </a>`).join('')}
      </div>
    </section>`);

  // ---------------------------------------------------------------------------
  // Console ao vivo: busca novas linhas de log a cada segundo
  // ---------------------------------------------------------------------------

  const consoleEl = document.getElementById('pn-console');
  let ultimoId = 0;
  let consoleVazio = true;
  const ouvintes = []; // quem quer ser avisado de novas linhas (cards 3 e 5)

  function ouvirLogs(filtro, callback) {
    const ouvinte = { filtro, callback };
    ouvintes.push(ouvinte);
    return () => ouvintes.splice(ouvintes.indexOf(ouvinte), 1);
  }

  async function buscarLogs() {
    try {
      const linhas = await getJson('/api/painel/logs?depoisDe=' + ultimoId);
      for (const linha of linhas) {
        ultimoId = Math.max(ultimoId, linha.id);
        if (consoleVazio) {
          consoleEl.innerHTML = '';
          consoleVazio = false;
        }
        consoleEl.insertAdjacentHTML('beforeend',
          '<div class="linha"><span class="hora">' + horaCurta(linha.hora) + '</span> ' +
          '<span class="thread">[' + esc(linha.thread) + ']</span> ' +
          '<span class="' + esc(linha.origem) + '">' + esc(linha.mensagem) + '</span></div>');
        ouvintes.slice().forEach((o) => o.filtro(linha) && o.callback(linha));
      }
      if (linhas.length) {
        consoleEl.scrollTop = consoleEl.scrollHeight;
      }
    }
    catch (e) {
      // servidor reiniciando: tenta de novo no próximo ciclo
    }
  }

  document.getElementById('pn-limpar').addEventListener('click', () => {
    consoleEl.innerHTML = '';
  });

  // Ignora o que já estava no log antes de abrir a página
  getJson('/api/painel/logs?depoisDe=0').then((linhas) => {
    linhas.forEach((l) => { ultimoId = Math.max(ultimoId, l.id); });
  }).catch(() => {}).finally(() => setInterval(buscarLogs, 1000));

  // ---------------------------------------------------------------------------
  // Ambiente
  // ---------------------------------------------------------------------------

  getJson('/api/painel/ambiente').then((a) => {
    const neon = /neon\.tech/.test(a.bancoHost) ? ' (Neon)' : '';
    document.getElementById('pn-ambiente').innerHTML = [
      'Java <b>' + esc(a.java) + '</b>',
      'Spring Boot <b>' + esc(a.springBoot) + '</b>',
      'Banco <b>' + esc(a.banco) + ' ' + esc(String(a.bancoVersao).split(' ')[0]) + neon + '</b>',
      'Virtual threads <b>' + (a.virtualThreads ? 'ligadas' : 'desligadas') + '</b>',
      'Profiles <b>' + esc(a.profiles.length ? a.profiles.join(', ') : 'default') + '</b>'
    ].map((t) => '<span class="pn-chip">' + t + '</span>').join('') +
      '<div class="small mt-1" style="opacity:.75">' + esc(a.bancoHost) + '</div>';
  }).catch((e) => erro(document.getElementById('pn-ambiente'), e));

  // ---------------------------------------------------------------------------
  // Extensão 1: gRPC
  // ---------------------------------------------------------------------------

  document.getElementById('pn-grpc').addEventListener('click', async () => {
    const alvo = document.getElementById('pn-grpc-resultado');
    alvo.innerHTML = '<span class="pn-vazio">chamando...</span>';
    try {
      const r = await getJson('/api/painel/grpc');
      const economia = Math.round(100 - (r.bytesProtobuf * 100) / r.bytesJson);
      alvo.innerHTML = `
        <div class="pn-metricas">
          <div class="pn-metrica"><span class="valor">${r.duracaoMs} ms</span><span class="rotulo">ida e volta até ${esc(r.alvo)}</span></div>
          <div class="pn-metrica"><span class="valor">${r.bytesProtobuf} B</span><span class="rotulo">resposta em Protobuf</span></div>
          <div class="pn-metrica"><span class="valor">${r.bytesJson} B</span><span class="rotulo">mesmos dados em JSON</span></div>
          <div class="pn-metrica"><span class="valor">${economia}%</span><span class="rotulo">menor em Protobuf</span></div>
        </div>
        <table class="pn-tabela"><tr><th>#</th><th>Veterinário</th><th>Especialidades</th></tr>
          ${r.veterinarios.map((v) => `<tr><td>${v.id}</td><td>${esc(v.nome)} ${esc(v.sobrenome)}</td>
            <td>${v.especialidades.length ? v.especialidades.map(esc).join(', ') : '<span class="pn-vazio">nenhuma</span>'}</td></tr>`).join('')}
        </table>`;
    }
    catch (e) {
      erro(alvo, e);
    }
  });

  // ---------------------------------------------------------------------------
  // Extensão 2: thread e benchmark
  // ---------------------------------------------------------------------------

  document.getElementById('pn-thread').addEventListener('click', async () => {
    const alvo = document.getElementById('pn-thread-resultado');
    try {
      const t = await getJson('/thread');
      alvo.innerHTML = `
        <p class="mb-1">Nome: <code>${esc(t.nome)}</code>
          <span class="pn-badge ${t.virtual ? 'pn-ok' : 'pn-nao'}">${t.virtual ? 'virtual thread' : 'thread de plataforma'}</span></p>
        <pre class="pn-codigo">${esc(t.detalhes)}</pre>
        <p class="small text-muted mt-1 mb-0">${t.virtual
          ? 'Depois da "/" aparece a <b>carrier thread</b>, a thread real do SO (ForkJoinPool) em que a virtual thread está montada agora. Clique de novo: a carrier pode mudar.'
          : 'As virtual threads estão desligadas nesta execução.'}</p>`;
    }
    catch (e) {
      erro(alvo, e);
    }
  });

  document.getElementById('pn-benchmark').addEventListener('click', async (evento) => {
    const botao = evento.currentTarget;
    const alvo = document.getElementById('pn-benchmark-resultado');
    const tarefas = Number(document.getElementById('pn-tarefas').value);
    const estimativa = Math.ceil(tarefas / 200) + 1;
    botao.disabled = true;
    alvo.innerHTML = `<span class="pn-vazio">rodando... cerca de ${estimativa} s (${tarefas} / 200 rodadas de 1 s no pool fixo, mais cerca de 1 s com virtual threads)</span>`;
    try {
      const resposta = await fetch('/api/painel/benchmark?tarefas=' + tarefas);
      if (resposta.status === 429) {
        throw new Error('Já existe um benchmark rodando. Aguarde alguns segundos.');
      }
      if (!resposta.ok) {
        throw new Error('HTTP ' + resposta.status);
      }
      const r = await resposta.json();
      const maior = Math.max(r.plataformaMs, r.virtualMs);
      const barra = (rotulo, ms, cor) => `
        <div class="pn-barra"><div class="rotulo"><span>${rotulo}</span><b>${ms.toLocaleString('pt-BR')} ms</b></div>
          <div class="trilho"><div class="preenchido" style="width:0;background:${cor}" data-largura="${Math.max(2, (ms * 100) / maior)}%"></div></div></div>`;
      alvo.innerHTML = barra(`Pool fixo (${r.threadsNoPool} threads de plataforma)`, r.plataformaMs, '#d9822b') +
        barra('Virtual threads (uma por tarefa)', r.virtualMs, '#6db33f') +
        `<p class="small text-muted mt-1 mb-0">${r.tarefas.toLocaleString('pt-BR')} tarefas: virtual threads foram
         <b>${(r.plataformaMs / r.virtualMs).toFixed(1)}x</b> mais rápidas, com o mesmo código de tarefa.</p>`;
      requestAnimationFrame(() => alvo.querySelectorAll('.preenchido').forEach((b) => { b.style.width = b.dataset.largura; }));
    }
    catch (e) {
      erro(alvo, e);
    }
    finally {
      botao.disabled = false;
    }
  });

  // ---------------------------------------------------------------------------
  // Extensões 3 e 4: pets, preços e registro de visita
  // ---------------------------------------------------------------------------

  getJson('/api/painel/pets').then((pets) => {
    document.getElementById('pn-precos').innerHTML = `
      <table class="pn-tabela"><tr><th>Pet</th><th>Tipo</th><th>Idade</th><th>Estratégia escolhida</th><th>Preço</th><th></th></tr>
        ${pets.map((p) => `<tr>
          <td>${esc(p.pet)}<br><span class="small text-muted">${esc(p.dono)}</span></td>
          <td>${esc(p.tipo)}</td><td>${p.idade}</td>
          <td><span class="pn-badge pn-est-${esc(p.estrategia)}">${esc(p.estrategia)}</span></td>
          <td><b>${moeda(p.preco)}</b></td>
          <td><a href="/owners/${p.donoId}/pets/${p.petId}/visits/new?lang=pt" target="_blank" title="abrir a tela real de nova visita">tela</a></td>
        </tr>`).join('')}
      </table>`;
    document.getElementById('pn-visita-pet').innerHTML = pets.map((p) =>
      `<option value="${p.donoId}/${p.petId}">${esc(p.pet)} (${esc(p.tipo)}) de ${esc(p.dono)}</option>`).join('');
  }).catch((e) => erro(document.getElementById('pn-precos'), e));

  document.getElementById('pn-visita').addEventListener('click', async (evento) => {
    const botao = evento.currentTarget;
    const alvo = document.getElementById('pn-visita-resultado');
    const [donoId, petId] = document.getElementById('pn-visita-pet').value.split('/');
    const amanha = new Date(Date.now() + 86400000);
    const data = amanha.getFullYear() + '-' + String(amanha.getMonth() + 1).padStart(2, '0') + '-' + String(amanha.getDate()).padStart(2, '0');
    const formulario = new URLSearchParams({ date: data, description: document.getElementById('pn-visita-descricao').value });

    botao.disabled = true;
    alvo.innerHTML = '<span class="pn-vazio">enviando...</span><ul class="pn-timeline" id="pn-timeline"></ul>';
    const timeline = document.getElementById('pn-timeline');

    // Acompanha os logs gerados por esta visita: [AUDITORIA] do controller e [Observer] do listener
    let primeiraHora = null;
    const pararDeOuvir = ouvirLogs(
      (l) => /processNewVisitForm/.test(l.mensagem) || l.origem === 'LembreteVisitaListener',
      (l) => {
        const hora = new Date(l.hora).getTime();
        primeiraHora = primeiraHora ?? hora;
        const ehAuditoria = l.origem === 'AuditoriaAspect';
        timeline.insertAdjacentHTML('beforeend',
          `<li class="${ehAuditoria ? 'auditoria' : ''}"><span class="delta">+${hora - primeiraHora} ms</span>
           <code>${esc(l.thread)}</code> ${esc(l.mensagem.length > 160 ? l.mensagem.slice(0, 160) + '…' : l.mensagem)}</li>`);
        if (/lembrete enviado/.test(l.mensagem)) {
          pararDeOuvir();
          alvo.insertAdjacentHTML('beforeend', '<p class="small text-muted mb-0">Repare: a resposta HTTP voltou antes do lembrete terminar. A thread do lembrete (<code>task-N</code>) é diferente da thread da requisição (<code>tomcat-handler-N</code>).</p>');
        }
      });

    const inicio = performance.now();
    try {
      // O mesmo POST do formulário do PetClinic. redirect: 'manual' para medir só esta requisição.
      const resposta = await fetch(`/owners/${donoId}/pets/${petId}/visits/new`, {
        method: 'POST', body: formulario, redirect: 'manual'
      });
      const ms = Math.round(performance.now() - inicio);
      const sucesso = resposta.type === 'opaqueredirect' || resposta.redirected || (resposta.status >= 300 && resposta.status < 400);
      alvo.querySelector('.pn-vazio').outerHTML = sucesso
        ? `<div class="pn-metricas"><div class="pn-metrica"><span class="valor">${ms} ms</span><span class="rotulo">resposta HTTP (302, visita salva)</span></div>
             <div class="pn-metrica"><span class="valor">2.000 ms</span><span class="rotulo">tempo do "envio" do lembrete</span></div></div>`
        : '<div class="alert alert-warning py-2">O formulário recusou os dados (validação). Tente outra descrição.</div>';
      if (!sucesso) {
        pararDeOuvir();
      }
    }
    catch (e) {
      pararDeOuvir();
      erro(alvo, e);
    }
    finally {
      botao.disabled = false;
    }
  });

  // ---------------------------------------------------------------------------
  // Extensão 5: AOP
  // ---------------------------------------------------------------------------

  getJson('/api/painel/aop').then((controllers) => {
    document.getElementById('pn-aop').innerHTML = `
      <table class="pn-tabela"><tr><th>Bean</th><th>Classe em tempo de execução</th><th>Métodos @Auditado</th></tr>
        ${controllers.map((c) => `<tr>
          <td>${esc(c.bean)}</td>
          <td><code>${esc(c.classeEmTempoDeExecucao)}</code><br>
            <span class="pn-badge ${c.proxy ? 'pn-ok' : 'pn-nao'}">${c.proxy ? 'proxy AOP' : 'objeto comum'}</span></td>
          <td>${c.metodosAuditados.length ? c.metodosAuditados.map((m) => '<code>' + esc(m) + '</code>').join('<br>') : '<span class="pn-vazio">nenhum</span>'}</td>
        </tr>`).join('')}
      </table>`;
  }).catch((e) => erro(document.getElementById('pn-aop'), e));

  document.querySelectorAll('[data-auditar]').forEach((botao) => {
    botao.addEventListener('click', async () => {
      const url = botao.dataset.auditar;
      const alvo = document.getElementById('pn-aop-resultado');
      alvo.innerHTML = '<ul class="pn-timeline" id="pn-aop-linhas"></ul>';
      const lista = document.getElementById('pn-aop-linhas');
      let recebeu = false;
      const parar = ouvirLogs((l) => l.origem === 'AuditoriaAspect', (l) => {
        recebeu = true;
        lista.insertAdjacentHTML('beforeend', `<li class="auditoria">${esc(l.mensagem.length > 220 ? l.mensagem.slice(0, 220) + '…' : l.mensagem)}</li>`);
      });
      try {
        const resposta = await fetch(url);
        lista.insertAdjacentHTML('beforebegin', `<p class="small mb-1">GET ${esc(url)} &rarr; HTTP ${resposta.status}</p>`);
      }
      catch (e) {
        erro(alvo, e);
      }
      // espera o próximo ciclo do console (1 s) para mostrar o que o aspecto registrou
      setTimeout(() => {
        parar();
        if (!recebeu) {
          lista.insertAdjacentHTML('beforeend', '<li>Nenhuma linha [AUDITORIA]: este método não tem @Auditado, então o aspecto não o intercepta.</li>');
        }
      }, 2500);
    });
  });

  // ---------------------------------------------------------------------------
  // Tópico 1: genéricos
  // ---------------------------------------------------------------------------

  document.getElementById('pn-genericos').addEventListener('click', async () => {
    const alvo = document.getElementById('pn-genericos-resultado');
    try {
      const g = await getJson('/api/painel/genericos');
      alvo.innerHTML = `
        <p class="mb-1"><b>1. Type erasure:</b> <code>new ArrayList&lt;String&gt;().getClass() == new ArrayList&lt;Integer&gt;().getClass()</code>
          &rarr; <span class="pn-badge ${g.mesmaClasse ? 'pn-ok' : 'pn-nao'}">${g.mesmaClasse}</span>
          <span class="small text-muted">(as duas são <code>${esc(g.classeDeListString)}</code>; o compilador apaga o tipo)</span></p>
        <p class="mb-1"><b>2. Mas o tipo fica nas assinaturas.</b> O Spring Data lê o genérico de cada interface:</p>
        <pre class="pn-codigo">${g.repositorios.map(esc).join('\n')}</pre>
        <p class="mb-1 mt-2"><b>3. E injeta por tipo genérico:</b> <code>${esc(g.injecaoPorTipoGenerico)}</code>
          recebeu <b>${g.estrategiasInjetadas}</b> beans, porque só eles combinam com o tipo do valor do Map.</p>
        <p class="mb-0"><b>4. Proxy gerado:</b> <code>VetRepository</code> não tem classe de implementação. Em tempo de execução ele é
          <code>${esc(g.vetRepositoryEmTempoDeExecucao)}</code>, criado a partir de <code>Repository&lt;Vet, Integer&gt;</code>.</p>`;
    }
    catch (e) {
      erro(alvo, e);
    }
  });

  // ---------------------------------------------------------------------------
  // Diagramas
  // ---------------------------------------------------------------------------

  // Usa a mesma fonte da página e só desenha depois que ela carregar. Se o Mermaid medir os
  // rótulos com outra fonte, o texto sai cortado.
  if (window.mermaid) {
    window.mermaid.initialize({
      startOnLoad: false,
      theme: 'neutral',
      securityLevel: 'strict',
      fontFamily: '"varela_roundregular", sans-serif'
    });
    document.fonts.ready.then(() => window.mermaid.run({ querySelector: '#painel pre.mermaid' }));
  }
})();
