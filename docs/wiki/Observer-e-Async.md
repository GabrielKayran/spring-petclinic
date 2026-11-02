# Observer com @Async

[← Início](Home.md)

## O problema

Ao registrar uma visita, o sistema deve avisar o tutor (e-mail, SMS...). Se o `VisitController` chamar o envio diretamente, ele passa a depender do serviço de mensagens, e o usuário espera o envio terminar para ver a página.

## A solução

O controller só **publica um evento**. Quem quiser reagir se inscreve. O listener roda em outra thread, então a página volta na hora.

| Papel no Observer | Classe |
|---|---|
| Sujeito (publica) | `VisitController` |
| Evento | `VisitRegisteredEvent` |
| Observador | `VisitReminderListener` |
| Barramento | `ApplicationEventPublisher` do Spring |

Diagramas: [UML 06, classes do Observer](../uml/svg/06_classes_observer.svg) e [UML 04, sequência do registro de visita](../uml/svg/04_sequencia_registro_visita.svg).

## 1. O sujeito publica

[VisitController.java](../../src/main/java/org/springframework/samples/petclinic/owner/VisitController.java#L103-L123)

```java
@Audited("Registrar visita")
@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
public String processNewVisitForm(@ModelAttribute Owner owner, @PathVariable int petId, @Valid Visit visit,
		BindingResult result, RedirectAttributes redirectAttributes) {
	if (visit.getDate() != null && !visit.getDate().isAfter(LocalDate.now())) {
		result.rejectValue("date", "typeMismatch.visitDate");
	}

	if (result.hasErrors()) {
		return "pets/createOrUpdateVisitForm";
	}

	owner.addVisit(petId, visit);
	this.owners.save(owner);

	this.events.publishEvent(new VisitRegisteredEvent(this, owner.getFirstName() + " " + owner.getLastName(),
			owner.getPet(petId).getName(), visit.getDate(), visit.getDescription()));

	redirectAttributes.addFlashAttribute("message", "visitBooked");
	return "redirect:/owners/{ownerId}";
}
```

- Linha 116: salva no banco.
- Linha 118: publica o evento. O controller não sabe quem vai ouvir.
- O `ApplicationEventPublisher` chega pelo construtor (injeção de dependência).

## 2. O evento

[VisitRegisteredEvent.java](../../src/main/java/org/springframework/samples/petclinic/reminder/VisitRegisteredEvent.java#L26-L43): um objeto imutável com os dados de que o observador precisa.

```java
public class VisitRegisteredEvent extends ApplicationEvent {

	private final String ownerName;

	private final String petName;

	private final LocalDate visitDate;

	private final String description;

	public VisitRegisteredEvent(Object source, String ownerName, String petName, LocalDate visitDate,
			String description) {
		super(source);
		...
	}
```

O evento leva `String`s e não a entidade `Owner`, porque o listener roda em outra thread, depois que a transação do controller já terminou.

## 3. O observador reage em outra thread

[VisitReminderListener.java](../../src/main/java/org/springframework/samples/petclinic/reminder/VisitReminderListener.java#L30-L50)

```java
@Component
public class VisitReminderListener {

	static final Duration SEND_DELAY = Duration.ofSeconds(2);

	@Async
	@EventListener
	public void sendReminder(VisitRegisteredEvent event) throws InterruptedException {
		Thread thread = Thread.currentThread();
		log.info("[Observer] Thread '{}' (virtual={}): preparando lembrete para {} sobre a visita de {} em {}",
				thread.getName(), thread.isVirtual(), event.getOwnerName(), event.getPetName(), event.getVisitDate());

		Thread.sleep(SEND_DELAY);

		log.info("[Observer] Thread '{}': lembrete enviado para {} ({})", thread.getName(), event.getOwnerName(),
				event.getDescription());
	}

}
```

- `@EventListener`: o Spring descobre pelo **tipo do parâmetro** que este método ouve `VisitRegisteredEvent`.
- `@Async`: o método roda num executor separado. Sem ele, o usuário esperaria os 2 s.
- `Thread.sleep(SEND_DELAY)` simula a demora de um envio real.

O `@Async` só funciona com `@EnableAsync` ligado, em [AsyncConfiguration.java](../../src/main/java/org/springframework/samples/petclinic/reminder/AsyncConfiguration.java#L25-L29):

```java
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfiguration {

}
```

## O que aparece no log

```
[AUDITORIA] VisitController.processNewVisitForm(..) (Registrar visita) | ... | 45 ms | thread=tomcat-handler-7
[Observer] Thread 'task-3' (virtual=true): preparando lembrete para George Franklin sobre a visita de Leo em 2026-11-03
[Observer] Thread 'task-3': lembrete enviado para George Franklin (Vacina anual)
```

A requisição roda em `tomcat-handler-N` e termina; o lembrete roda em `task-N`, 2 s depois. As duas são virtual threads por causa de `spring.threads.virtual.enabled=true`. No painel, o card 3 registra uma visita e mostra essas linhas no console ao vivo.

## Por que é Observer

- O sujeito não conhece o observador: dá para adicionar um `VisitSmsListener` sem tocar no `VisitController` (aberto/fechado).
- Vários observadores podem ouvir o mesmo evento.
- O acoplamento fica só no tipo `VisitRegisteredEvent`.

## Teste

[VisitRegisteredEventTests.java](../../src/test/java/org/springframework/samples/petclinic/reminder/VisitRegisteredEventTests.java#L53-L75) prova as três coisas: o evento é publicado, tem os dados certos e o lembrete roda em outra thread.

```java
List<VisitRegisteredEvent> published = this.events.stream(VisitRegisteredEvent.class).toList();
assertThat(published).hasSize(1);
VisitRegisteredEvent event = published.get(0);
assertThat(event.getOwnerName()).isEqualTo("George Franklin");
assertThat(event.getPetName()).isEqualTo("Leo");

await().atMost(Duration.ofSeconds(10))
	.untilAsserted(() -> assertThat(output.getOut()).contains("lembrete enviado para George Franklin"));
assertThat(output.getOut()).contains("[Observer] Thread 'task-");
assertThat(output.getOut()).doesNotContain("[Observer] Thread '" + Thread.currentThread().getName() + "'");
```
