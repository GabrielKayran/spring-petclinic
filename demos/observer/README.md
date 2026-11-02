# Demo: Observer com carro roubado e viaturas

O padrão Observer sozinho, sem Spring. O carro roubado (`StolenCar`) é o sujeito e guarda uma lista de observadores. Toda vez que ele anda, chama `update()` em cada um. A viatura (`PoliceCar`) e o helicóptero (`Helicopter`) reagem cada um do seu jeito.

| Classe | Papel |
|---|---|
| `Observer` | interface com `update(StolenCar car)` |
| `StolenCar` | sujeito: `attach`, `detach`, `move` e `notifyObservers` |
| `PoliceCar` | observador: vai até a posição avisada |
| `Helicopter` | observador: ilumina a posição avisada |

## Como rodar (JDK 21+)

```bash
cd demos/observer
java PoliceChase.java
```

Saída:

```
Viatura 1: carro ROB-0001 visto em (1, 0), indo para lá
Viatura 2: carro ROB-0001 visto em (1, 0), indo para lá
Helicóptero: iluminando o carro ROB-0001 em (1, 0)
-- Viatura 2 sai da escuta do rádio --
Viatura 1: carro ROB-0001 visto em (1, 1), indo para lá
Helicóptero: iluminando o carro ROB-0001 em (1, 1)
```

## Versão visual

A mesma classe vira um jogo em `/painel/observer`. Você dirige o carro roubado pelas setas, liga e desliga viaturas da escuta com as teclas `1` a `5` e acompanha, ao lado do mapa, a lista `observers`, as chamadas e as linhas deste arquivo sendo executadas.
