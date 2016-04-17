/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RedisHealthIndicator}.
 *
 * @author Christian Dupuis
 */
public class RedisHealthIndicatorTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void indicatorExists() {
		this.context = new AnnotationConfigApplicationContext(
				PropertyPlaceholderAutoConfiguration.class, RedisAutoConfiguration.class,
				EndpointAutoConfiguration.class, HealthIndicatorAutoConfiguration.class);
		assertThat(this.context.getBeanNamesForType(RedisConnectionFactory.class))
				.hasSize(1);
		RedisHealthIndicator healthIndicator = this.context
				.getBean(RedisHealthIndicator.class);
		assertThat(healthIndicator).isNotNull();
	}

	@Test
	public void redisIsUp() throws Exception {
		Properties info = new Properties();
		info.put("redis_version", "2.8.9");
		RedisConnection redisConnection = mock(RedisConnection.class);
		RedisConnectionFactory redisConnectionFactory = mock(
				RedisConnectionFactory.class);
		given(redisConnectionFactory.getConnection()).willReturn(redisConnection);
		given(redisConnection.info()).willReturn(info);
		RedisHealthIndicator healthIndicator = new RedisHealthIndicator(
				redisConnectionFactory);
		HealthInterface health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("version")).isEqualTo("2.8.9");
		verify(redisConnectionFactory).getConnection();
		verify(redisConnection).info();
	}

	@Test
	public void redisIsDown() throws Exception {
		RedisConnection redisConnection = mock(RedisConnection.class);
		RedisConnectionFactory redisConnectionFactory = mock(
				RedisConnectionFactory.class);
		given(redisConnectionFactory.getConnection()).willReturn(redisConnection);
		given(redisConnection.info())
				.willThrow(new RedisConnectionFailureException("Connection failed"));
		RedisHealthIndicator healthIndicator = new RedisHealthIndicator(
				redisConnectionFactory);
		HealthInterface health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(((String) health.getDetails().get("error"))
				.contains("Connection failed"));
		verify(redisConnectionFactory).getConnection();
		verify(redisConnection).info();
	}

}
