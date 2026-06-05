package com.commerce.api.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 설정.
 *
 * <p>{@link JPAQueryFactory}는 EntityManager를 감싸 "타입 안전한 쿼리"를 만드는 진입점이다.
 * 한 번 빈으로 등록해 두면, QueryDSL을 쓰는 리포지토리 구현체에서 주입받아 재사용할 수 있다.
 * (.NET 비유: EF의 DbContext에서 IQueryable 쿼리를 시작하는 진입점과 비슷한 역할)
 */
@Configuration
public class QuerydslConfig {

    /**
     * JPAQueryFactory 빈.
     *
     * @param em 스프링이 관리하는 영속성 컨텍스트. 메서드 파라미터로 두면 스프링이 자동 주입한다.
     *           (요청/트랜잭션마다 올바른 EntityManager로 위임되는 공유 프록시가 들어온다)
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
