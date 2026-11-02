# AOP: auditoria com @Audited

[← Início](Home.md)

## O problema

Registrar quem chamou o quê, com quais argumentos e quanto tempo levou é um **interesse transversal** (*cross-cutting concern*): aparece em vários controllers. Escrever o log dentro de cada método espalha código repetido.

## A solução

Uma anotação marca **onde** auditar, e um aspecto define **o que** fazer. Diagrama: [UML 07, classes do AOP](../uml/svg/07_classes_aop.svg).

| Termo de AOP | No projeto |
|---|---|
| Aspecto | `AuditAspect` |
| Join point | a execução de um método anotado |
| Pointcut | `@annotation(audited)` |
| Advice | o método `audit`, do tipo `@Around` |
| Weaving | Spring AOP, por proxy, na hora em que o bean é criado |

## A anotação

[Audited.java](../../src/main/java/org/springframework/samples/petclinic/audit/Audited.java#L25-L33)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

	String value() default "";

}
```

`RUNTIME` é obrigatório: sem ele a anotação some na compilação e o aspecto não a enxerga.

## O aspecto

[AuditAspect.java](../../src/main/java/org/springframework/samples/petclinic/audit/AuditAspect.java#L31-L53)

```java
@Aspect
@Component
public class AuditAspect {

	private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

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
			log.info("[AUDITORIA] {} {} | args={} | {} ms | thread={}", method,
					audited.value().isEmpty() ? "" : "(" + audited.value() + ")", args, durationMs,
					Thread.currentThread().getName());
		}
	}

}
```

- `@Around` envolve o método: executa código antes, chama o original com `proceed()` e executa código depois.
- `@annotation(audited)` liga o parâmetro `Audited audited` à anotação do método, então o aspecto lê o texto `"Registrar visita"`.
- O `finally` garante o log mesmo se o método lançar exceção.

## Onde é usado

| Método | Rótulo |
|---|---|
| [OwnerController.showOwner](../../src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java#L179-L181) | `@Audited("Exibir dono")` |
| [VisitController.processNewVisitForm](../../src/main/java/org/springframework/samples/petclinic/owner/VisitController.java#L103-L105) | `@Audited("Registrar visita")` |
| [VetController.showResourcesVetList](../../src/main/java/org/springframework/samples/petclinic/vet/VetController.java#L73-L75) | `@Audited("Listar veterinários (JSON)")` |
| [ReportController.petPrices](../../src/main/java/org/springframework/samples/petclinic/report/ReportController.java#L46-L48) | `@Audited("Gerar relatório PDF (JasperReports)")` |

```java
@Audited("Exibir dono")
@GetMapping("/owners/{ownerId}")
public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
```

O controller não importa nenhuma classe de log. Tirar a anotação desliga a auditoria naquele método.

## Como funciona por baixo: proxies

O Spring não altera o bytecode da classe. Quando um bean tem método anotado, o contêiner cria uma **subclasse proxy** (CGLIB) e é ela que fica no contexto:

```
Chamada ──▶ VisitController$$SpringCGLIB$$0 (proxy) ──▶ AuditAspect.audit() ──▶ VisitController.processNewVisitForm()
```

O card 5 do painel lista todos os controllers e mostra quais são proxies. A lista vem de [DashboardController.java](../../src/main/java/org/springframework/samples/petclinic/dashboard/DashboardController.java#L169-L189):

```java
for (String name : this.context.getBeanNamesForAnnotation(Controller.class)) {
	Object bean = this.context.getBean(name);
	Class<?> targetClass = AopUtils.getTargetClass(bean);
	...
	List<String> audited = Arrays.stream(targetClass.getDeclaredMethods())
		.filter(m -> AnnotatedElementUtils.hasAnnotation(m, Audited.class))
		.map(m -> m.getName() + "()")
		.sorted()
		.toList();
	result.add(new ControllerProxy(name, bean.getClass().getSimpleName(), AopUtils.isAopProxy(bean), audited));
}
```

**Limitação do proxy:** se um método da própria classe chamar outro método anotado com `this.metodo()`, a chamada não passa pelo proxy e não é auditada. O AspectJ com compilador `ajc` não tem essa limitação, mas exige um passo de build especial.

## Spring AOP × AspectJ

| | AspectJ (`ajc`) | Spring AOP |
|---|---|---|
| Weaving | na compilação (bytecode alterado) | em tempo de execução, por proxy |
| Join points | métodos, campos, construtores | só execução de métodos públicos de beans |
| Build | compilador próprio | nenhuma mudança |
| Sintaxe | `@Aspect`, `@Around`, pointcuts | a mesma |

## Teste

[AuditAspectTests.java](../../src/test/java/org/springframework/samples/petclinic/audit/AuditAspectTests.java#L45-L64) confere que só os controllers com métodos auditados viram proxy e que a linha `[AUDITORIA]` aparece no log:

```java
void controllersWithAuditedMethodsAreProxied() {
	assertThat(AopUtils.isAopProxy(this.context.getBean("vetController"))).isTrue();
	assertThat(AopUtils.isAopProxy(this.context.getBean("ownerController"))).isTrue();
	assertThat(AopUtils.isAopProxy(this.context.getBean("visitController"))).isTrue();
	assertThat(AopUtils.isAopProxy(this.context.getBean("welcomeController"))).isFalse();
}
```
