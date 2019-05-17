package com.snb.deal.config;

import com.jianlc.schedule.strategy.JianlcScheduleManagerFactory;
import com.jianlc.tc.guid.GuidCreater;
import com.jianlc.tc.zkconfig.ConfigClient;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class BeanConfig {

    @Bean
    public ConfigClient configClient(Environment environment) {
        ConfigClient configClient = new ConfigClient(environment.getProperty("guid.configClient"));
        return configClient;
    }

    @Bean
    public GuidCreater guidCreater(Environment environment) {
        GuidCreater guidCreater = new GuidCreater(configClient(environment), environment.getProperty("guid.createrName"));
        return guidCreater;
    }

    /**
     * 方法级别的校验  要校验的方法所在类必须添加@Validated注解，方法参数加@Valid注解(也可以加@NotNull等) <br/>
     * 如果是有接口的service，需要加在接口的类上，而不是实现类上，参数实体中的属性中可以加@NotNull等注解 <br/>
     * 如果要手动校验，不需要在类加任何注解，只需要在bean的属性上加@NotNull等，然后用如下方法进行校验：<br/>
     * <pre>
     * GetThirdUserInfo.Request request = new GetThirdUserInfo.Request();
     * org.springframework.validation.BindingResult bindingResult = new org.springframework.validation.BeanPropertyBindingResult(request, "请求参数request");
     * beanFactory.getBean(org.springframework.validation.Validator).validate(request, bindingResult); // 也可以用@Autowired
     * if (bindingResult.hasErrors()){
     *     // 进行错误处理
     * } </pre>
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }



    @Bean
    @ConfigurationProperties(prefix = "zookeeper")
    public Map<String, String> zkConfig() {
        return new HashMap<>();
    }

    @Bean
    @DependsOn("zkConfig")
    public JianlcScheduleManagerFactory jianlcScheduleManagerFactory(BeanFactory beanFactory) throws Exception{
        JianlcScheduleManagerFactory factory = new JianlcScheduleManagerFactory();
        factory.setZkConfig((Map<String, String>) beanFactory.getBean("zkConfig"));
        factory.init();
        return factory;
    }
}
