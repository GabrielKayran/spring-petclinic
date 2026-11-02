# Relatório JasperReports

[← Início](Home.md)

O endpoint `GET /relatorios/pets-e-precos.pdf` gera um PDF com todos os pets, o tipo, a estratégia de preço aplicada, o dono, a idade, o número de visitas e o preço da consulta.

## As três partes

| Parte | Arquivo | Papel |
|---|---|---|
| Layout | [pets-and-prices.jrxml](../../src/main/resources/reports/pets-and-prices.jrxml) | XML com bandas (título, cabeçalho, detalhe, rodapé), editado no Jaspersoft Studio |
| Dados | [ReportRow.java](../../src/main/java/org/springframework/samples/petclinic/report/ReportRow.java) | um JavaBean por linha |
| Motor | [ReportService.java](../../src/main/java/org/springframework/samples/petclinic/report/ReportService.java) | compila, preenche e exporta |

## O layout declara campos e parâmetros

```xml
<parameter name="GENERATED_AT" class="java.lang.String"/>
<parameter name="DATABASE" class="java.lang.String"/>
<field name="type" class="java.lang.String"/>
<field name="strategy" class="java.lang.String"/>
<field name="pet" class="java.lang.String"/>
<field name="owner" class="java.lang.String"/>
<field name="age" class="java.lang.Integer"/>
<field name="visits" class="java.lang.Integer"/>
<field name="price" class="java.math.BigDecimal"/>
```

Cada `field` corresponde a um getter de `ReportRow` (`type` → `getType()`). Os parâmetros são valores únicos que vão no cabeçalho.

## Montando as linhas

[ReportService.java](../../src/main/java/org/springframework/samples/petclinic/report/ReportService.java#L105-L120)

```java
List<ReportRow> rows() {
	List<ReportRow> rows = new ArrayList<>();
	for (Owner owner : this.owners.findByLastNameStartingWith("", PageRequest.of(0, 100))) {
		for (Pet pet : owner.getPets()) {
			VisitPriceStrategy strategy = this.visitPriceService.strategyFor(pet);
			int age = (pet.getBirthDate() == null) ? 0
					: Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
			rows.add(new ReportRow((pet.getType() == null) ? "?" : pet.getType().getName(),
					ClassUtils.getUserClass(strategy).getSimpleName(), pet.getName(),
					owner.getFirstName() + " " + owner.getLastName(), age, pet.getVisits().size(),
					strategy.calculatePrice(pet)));
		}
	}
	rows.sort(Comparator.comparing(ReportRow::getType).thenComparing(ReportRow::getPet));
	return rows;
}
```

O relatório reaproveita o [Strategy](Strategy-preco-da-consulta.md): a coluna "estratégia" mostra qual classe calculou cada preço.

## Preenchendo e exportando

[ReportService.java](../../src/main/java/org/springframework/samples/petclinic/report/ReportService.java#L94-L102)

```java
public byte[] generatePetPricesPdf(String database) throws JRException, IOException {
	Map<String, Object> parameters = new HashMap<>();
	parameters.put("GENERATED_AT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
	parameters.put("DATABASE", database);
	parameters.put(JRParameter.REPORT_LOCALE, PT_BR);

	JasperPrint filled = JasperFillManager.fillReport(layout(), parameters, new JRBeanCollectionDataSource(rows()));
	return JasperExportManager.exportReportToPdf(filled);
}
```

O ciclo do JasperReports é **compilar** (`.jrxml` → `JasperReport`), **preencher** (layout + parâmetros + dados → `JasperPrint`) e **exportar** (→ PDF). `REPORT_LOCALE` em pt-BR formata os preços como `R$ 120,00`.

A compilação é a etapa lenta, então é feita uma vez só e guardada (veja a dupla checagem em [Concorrência](Concorrencia-e-virtual-threads.md#outros-pontos-de-concorrência-no-código)).

## O endpoint

[ReportController.java](../../src/main/java/org/springframework/samples/petclinic/report/ReportController.java#L46-L61)

```java
@Audited("Gerar relatório PDF (JasperReports)")
@GetMapping("/relatorios/pets-e-precos.pdf")
ResponseEntity<byte[]> petPrices() throws JRException, IOException, SQLException {
	byte[] pdf = this.reportService.generatePetPricesPdf(databaseName());
	return ResponseEntity.ok()
		.contentType(MediaType.APPLICATION_PDF)
		.header(HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.inline().filename("pets-e-precos.pdf").build().toString())
		.body(pdf);
}

private String databaseName() throws SQLException {
	try (Connection connection = this.dataSource.getConnection()) {
		return connection.getMetaData().getDatabaseProductName();
	}
}
```

- `inline` faz o navegador abrir o PDF em vez de baixar.
- O nome do banco (H2 ou PostgreSQL) sai no cabeçalho do relatório, como prova de que os dados vieram do banco em uso.
- O método é auditado pelo [aspecto](AOP-auditoria.md).

## iReport × Jaspersoft Studio

O motor JasperReports continua o mesmo de antes; o editor visual iReport foi descontinuado e substituído pelo Jaspersoft Studio. O `.jrxml` abre nele.
