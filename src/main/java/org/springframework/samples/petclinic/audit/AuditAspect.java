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
package org.springframework.samples.petclinic.audit;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs the method, arguments and duration of every {@link Audited} method. Spring AOP
 * weaves it through a CGLIB proxy created at startup.
 */
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
