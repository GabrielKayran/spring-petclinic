# Wiki do Spring PetClinic (POO2 · Grupo 07)

Esta wiki descreve o fork do Spring PetClinic usado no estudo de caso de Programação Orientada a Objetos 2 (UFU). Cada página aponta para o arquivo e a linha do código real, e os trechos foram copiados do repositório.

**Integrantes:** Nathan Silva Neves, Ryan Carlos Cabral de Sousa, João Victor Bento Chagas Teixeira e Gabriel Kayran Silva.

**Sistema publicado:** https://petclinic-poo2.onrender.com (painel em `/painel`, saúde em `/health`).

## O que o projeto é

O PetClinic é a aplicação de exemplo oficial do Spring: uma clínica veterinária com tutores, pets, visitas e veterinários. Sobre essa base o grupo acrescentou cinco extensões, cada uma ligada a um tema da disciplina:

| Tema da disciplina | O que foi feito | Pacote |
|---|---|---|
| Objetos distribuídos (RMI) | Demo RMI clássica e serviço gRPC equivalente | `grpc`, `demos/rmi` |
| Concorrência | Virtual threads do Java 21 no Tomcat e no `@Async`, benchmark | `virtualthreads`, `dashboard` |
| Padrões de projeto | Observer (eventos + `@Async`) e Strategy (preço da consulta) | `reminder`, `pricing` |
| Programação orientada a aspectos | Anotação `@Audited` e aspecto `@Around` | `audit` |
| Frameworks e persistência | Spring MVC, JPA/Hibernate, PostgreSQL (Neon), relatório JasperReports | `owner`, `vet`, `report` |

## Páginas

1. [Arquitetura e pacotes](Arquitetura-e-pacotes.md): camadas, pacotes e como uma requisição atravessa o sistema
2. [Como executar](Como-executar.md): rodar local (H2), com PostgreSQL, Docker e o deploy no Render
3. [Modelo de domínio e banco de dados](Modelo-de-dominio-e-banco.md): entidades JPA, herança, repositórios genéricos e o SQL que o Hibernate gera
4. [RMI e gRPC](RMI-e-gRPC.md): a mesma chamada remota nas duas tecnologias
5. [Concorrência e virtual threads](Concorrencia-e-virtual-threads.md): threads virtuais, benchmark, `Semaphore` e sincronização
6. [Observer com @Async](Observer-e-Async.md): lembrete de visita enviado em outra thread
7. [Strategy: preço da consulta](Strategy-preco-da-consulta.md): uma estratégia por tipo de animal, escolhida pelo Spring
8. [AOP: auditoria](AOP-auditoria.md): `@Audited`, pointcut, advice e proxies
9. [Relatório JasperReports](Relatorio-JasperReports.md): PDF de pets e preços
10. [Painel e API](Painel-e-API.md): a página `/painel` e todos os endpoints JSON
11. [Testes](Testes.md): o que cada teste prova e como rodar
12. [Diagramas UML](Diagramas-UML.md): os 11 diagramas e a ligação com o código

## Stack

| Item | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.0 |
| Spring gRPC | `spring-boot-starter-grpc-server` |
| Banco | H2 (local), PostgreSQL no Neon (produção), MySQL opcional |
| Relatórios | JasperReports + OpenPDF |
| Front-end | Thymeleaf, Bootstrap, CSS próprio (`site.css`, `dashboard.css`) |
| Hospedagem | Render (Docker, plano free) |
