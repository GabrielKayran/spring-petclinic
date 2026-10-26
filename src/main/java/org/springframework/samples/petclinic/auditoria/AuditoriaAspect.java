/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.auditoria;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Extensão 5: aspecto de auditoria.
 * <p>
 * Vocabulário de AOP:
 * <ul>
 * <li><b>Aspect</b>: esta classe, que reúne o comportamento transversal.</li>
 * <li><b>Pointcut</b>: {@code @annotation(auditado)}, que seleciona os métodos anotados
 * com {@link Auditado}.</li>
 * <li><b>Advice</b>: o método {@link #auditar}, do tipo {@code @Around}, que roda antes e
 * depois do método original.</li>
 * <li><b>Join point</b>: cada chamada interceptada ({@link ProceedingJoinPoint}).</li>
 * <li><b>Weaving</b>: o Spring cria, na inicialização, um <i>proxy</i> (subclasse gerada
 * com CGLIB) do controller. Quem recebe a requisição é o proxy, que chama este advice,
 * que por sua vez chama o método real. O AspectJ "de verdade" faria o weaving no
 * bytecode, com um compilador especial (ajc) ou um agente na JVM.</li>
 * </ul>
 */
@Aspect
@Component
public class AuditoriaAspect {

	private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

	@Around("@annotation(auditado)")
	public Object auditar(ProceedingJoinPoint joinPoint, Auditado auditado) throws Throwable {
		String metodo = joinPoint.getSignature().toShortString();
		String argumentos = Arrays.toString(joinPoint.getArgs());
		long inicio = System.nanoTime();
		try {
			// proceed() executa o método original (ou o próximo aspecto da cadeia)
			return joinPoint.proceed();
		}
		finally {
			// finally: registra o tempo mesmo quando o método lança exceção
			long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;
			log.info("[AUDITORIA] {} {} | args={} | {} ms | thread={}", metodo,
					auditado.value().isEmpty() ? "" : "(" + auditado.value() + ")", argumentos, duracaoMs,
					Thread.currentThread().getName());
		}
	}

}
