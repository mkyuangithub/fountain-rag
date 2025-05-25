package com.mkyuan.fountaingateway.config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered{
    protected Logger logger = LogManager.getLogger(this.getClass());

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("====== Gateway Filter Start ======");
        logger.info("Request path: " + exchange.getRequest().getPath());
        logger.info("Request URI: " + exchange.getRequest().getURI());
        logger.info("Request method: " + exchange.getRequest().getMethod());
        logger.info("Request headers: " + exchange.getRequest().getHeaders());

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    logger.info("====== Gateway Filter End ======");
                    logger.info("Response status: " + exchange.getResponse().getStatusCode());
                    logger.info("Response headers: " + exchange.getResponse().getHeaders());
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;  // 确保最先执行
    }
}
