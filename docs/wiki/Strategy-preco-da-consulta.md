# Strategy: preço da consulta

[← Início](Home.md)

## O problema

O preço de uma consulta depende do animal. A solução ingênua é um `if`/`switch` por tipo, que precisa ser editado a cada animal novo.

## A solução

Uma interface, uma classe por regra e um serviço que escolhe a regra certa. Diagrama: [UML 05, classes do Strategy](../uml/svg/05_classes_strategy.svg).

```
VisitPriceService ──usa──▶ «interface» VisitPriceStrategy
                                ▲            ▲             ▲
                    DogPriceStrategy  CatPriceStrategy  DefaultPriceStrategy
                       @Component("dog")  ("cat")         ("default")
```

## A interface

[VisitPriceStrategy.java](../../src/main/java/org/springframework/samples/petclinic/pricing/VisitPriceStrategy.java#L26-L30)

```java
public interface VisitPriceStrategy {

	BigDecimal calculatePrice(Pet pet);

}
```

## As estratégias

[DogPriceStrategy.java](../../src/main/java/org/springframework/samples/petclinic/pricing/DogPriceStrategy.java#L26-L39): R$ 100 mais R$ 5 por ano de idade.

```java
@Component("dog")
class DogPriceStrategy implements VisitPriceStrategy {

	private static final BigDecimal BASE_PRICE = new BigDecimal("100.00");

	private static final BigDecimal PER_YEAR_OF_AGE = new BigDecimal("5.00");

	@Override
	public BigDecimal calculatePrice(Pet pet) {
		int age = (pet.getBirthDate() == null) ? 0 : Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
		return BASE_PRICE.add(PER_YEAR_OF_AGE.multiply(BigDecimal.valueOf(age)));
	}

}
```

[CatPriceStrategy.java](../../src/main/java/org/springframework/samples/petclinic/pricing/CatPriceStrategy.java#L23-L33): preço fixo de R$ 90.

```java
@Component("cat")
class CatPriceStrategy implements VisitPriceStrategy {

	private static final BigDecimal PRICE = new BigDecimal("90.00");

	@Override
	public BigDecimal calculatePrice(Pet pet) {
		return PRICE;
	}

}
```

[DefaultPriceStrategy.java](../../src/main/java/org/springframework/samples/petclinic/pricing/DefaultPriceStrategy.java#L24-L36): R$ 70 para qualquer outro tipo.

```java
@Component(DefaultPriceStrategy.NAME)
class DefaultPriceStrategy implements VisitPriceStrategy {

	static final String NAME = "default";

	private static final BigDecimal PRICE = new BigDecimal("70.00");
	...
}
```

O detalhe importante é o **nome do bean**: `"dog"` e `"cat"` são exatamente os nomes da tabela `types` do banco.

## O contexto que escolhe

[VisitPriceService.java](../../src/main/java/org/springframework/samples/petclinic/pricing/VisitPriceService.java#L30-L51)

```java
@Service
public class VisitPriceService {

	private final Map<String, VisitPriceStrategy> strategiesByType;

	private final VisitPriceStrategy defaultStrategy;

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
	}

}
```

- Ao pedir um `Map<String, VisitPriceStrategy>` no construtor, o Spring entrega **todos** os beans que implementam a interface, indexados pelo nome: `{dog=DogPriceStrategy, cat=CatPriceStrategy, default=DefaultPriceStrategy}`.
- `getOrDefault` substitui o `switch`: tipo sem estratégia cai na padrão.
- O `Optional` trata pet sem tipo.

## Onde o preço aparece

Na tela de nova visita. Um interceptor coloca o preço no modelo sem que o `VisitController` precise saber dele ([VisitPriceWebConfiguration.java](../../src/main/java/org/springframework/samples/petclinic/pricing/VisitPriceWebConfiguration.java#L42-L54)):

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
	registry.addInterceptor(this).addPathPatterns("/owners/*/pets/*/visits/new");
}

@Override
public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
		ModelAndView modelAndView) {
	if (modelAndView != null && modelAndView.getModel().get("pet") instanceof Pet pet) {
		this.visitPriceService
			.ifAvailable(service -> modelAndView.addObject("visitPrice", service.calculatePrice(pet)));
	}
}
```

O preço também é usado no [relatório PDF](Relatorio-JasperReports.md) e na tabela do card 4 do painel.

## Criando uma estratégia nova (live coding)

Para cobrar R$ 60 de pássaros, basta criar **um arquivo**, sem editar nenhuma classe existente:

```java
@Component("bird")
class BirdPriceStrategy implements VisitPriceStrategy {

	@Override
	public BigDecimal calculatePrice(Pet pet) {
		return new BigDecimal("60.00");
	}

}
```

Reinicie, e o Spring passa a incluir `bird` no `Map`. Esse é o princípio aberto/fechado na prática.

## Teste

[VisitPriceServiceTests.java](../../src/test/java/org/springframework/samples/petclinic/pricing/VisitPriceServiceTests.java#L35-L56)

```java
@Test
void dogPaysBasePriceAndFiveReaisPerYear() {
	Pet rex = pet("dog", LocalDate.now().minusYears(4));
	assertThat(this.service.calculatePrice(rex)).isEqualByComparingTo("120.00");
}

@Test
void typesWithoutAStrategyUseTheDefault() {
	assertThat(this.service.calculatePrice(pet("hamster", LocalDate.now()))).isEqualByComparingTo("70.00");
	assertThat(this.service.calculatePrice(pet("lizard", LocalDate.now()))).isEqualByComparingTo("70.00");
}
```
