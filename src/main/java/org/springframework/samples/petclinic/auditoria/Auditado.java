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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extensão 5: marca um método para ser auditado pelo {@link AuditoriaAspect}.
 * <p>
 * Sozinha, a anotação não faz nada: é só um marcador. Quem age é o aspecto, que usa o
 * pointcut {@code @annotation(auditado)} para interceptar todos os métodos marcados.
 * Assim o código de log e de medição de tempo (um <i>cross-cutting concern</i>) fica num
 * único lugar, e não espalhado pelos controllers.
 * <p>
 * {@code RUNTIME} é obrigatório: o Spring AOP procura a anotação por reflexão com a
 * aplicação rodando.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditado {

	/** Descrição opcional da operação, que aparece no log. */
	String value() default "";

}
