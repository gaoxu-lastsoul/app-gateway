package last.soul.gateway.config;

import last.soul.gateway.handler.JsonSentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class GatewayConfiguration {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public GatewayConfiguration(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }


    /**
     * 配置SentinelGatewayBlockExceptionHandler，限流后异常处理
     *
     * @return
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public JsonSentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new JsonSentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 配置SentinelGatewayFilter
     *
     * @return
     */
    @Bean
    @Order(-1)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    @PostConstruct
    public void doInit() {
        initCustomizedApis();
        initGatewayRules();
    }

    /**
     * API分组，对不组可以进行不同限流规则
     */
    private void initCustomizedApis() {
        Set<ApiDefinition> definitions = new HashSet<>();
        ApiDefinition api1 = new ApiDefinition("appPush-service")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/appPush/**")
                    );
                }});
        ApiDefinition api2 = new ApiDefinition("appPush-service2")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/appPush2/**")
                    );
                }});
        definitions.add(api1);
        definitions.add(api2);
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    /**
     * 配置限流规则
     */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
//        rules.add(new GatewayFlowRule("path_route")
//                .setCount(1) // 限流阈值
//                .setIntervalSec(1) // 统计时间窗口，单位是秒，默认是 1 秒
//                .setParamItem(new GatewayParamFlowItem()
//                        //指定参数限流 ?vipType=xxx
//                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM).setFieldName("vipType")
//                )
//        );
        rules.add(new GatewayFlowRule("appPush-service")
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER)//0 直接拒绝 ,1 Warm Up, 2 匀速排队
                .setGrade(RuleConstant.FLOW_GRADE_QPS)//0 基于线程数,1 基于QPS
                .setMaxQueueingTimeoutMs(5000)//和排队数量、单个任务处理时间成正比。可以设置大点防止丢弃请求。
                .setCount(10)//qps限制为10
                .setIntervalSec(1)//时间窗口为1s
        );
        GatewayRuleManager.loadRules(rules);
    }
}