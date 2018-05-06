package cn.springcloud.feign;

import feign.Feign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ServletCookieValueMethodArgumentResolver;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnClass(Feign.class)
public class FeignAutoConfig {

    @Autowired
    private RequestMappingHandlerAdapter adapter;

    @Autowired
    private ConfigurableBeanFactory beanFactory;

    @Bean
    public FeignRequestInterceptor feignPlusRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

    @Bean
    public FeignSpringMvcContract feignPlusSpringMvcContract(@Autowired(required = false) List<AnnotatedParameterProcessor> parameterProcessors,
                                                             ConversionService conversionService) {
        if (null == parameterProcessors) {
            parameterProcessors = new ArrayList<>();
        }
        return new FeignSpringMvcContract(parameterProcessors, conversionService);
    }

    public static MethodParameter interfaceMethodParameter(MethodParameter parameter, Class annotationType) {
        if (!parameter.hasParameterAnnotation(annotationType)) {
            for (Class<?> itf : parameter.getDeclaringClass().getInterfaces()) {
                try {
                    Method method = itf.getMethod(parameter.getMethod().getName(), parameter.getMethod().getParameterTypes());
                    MethodParameter itfParameter = new MethodParameter(method, parameter.getParameterIndex());
                    if (itfParameter.hasParameterAnnotation(annotationType)) {
                        return itfParameter;
                    }
                } catch (NoSuchMethodException e) {
                    continue;
                }
            }
        }
        return parameter;
    }

    @PostConstruct
    public void modifyArgumentResolvers() {
        List<HandlerMethodArgumentResolver> list = new ArrayList<>(adapter.getArgumentResolvers());

        list.add(0, new PathVariableMethodArgumentResolver() {  // PathVariable 支持接口注解
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, PathVariable.class));
            }

            @Override
            protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
                return super.createNamedValueInfo(interfaceMethodParameter(parameter, PathVariable.class));
            }
        });

        list.add(0, new RequestHeaderMethodArgumentResolver(beanFactory) {  // RequestHeader 支持接口注解
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, RequestHeader.class));
            }

            @Override
            protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
                return super.createNamedValueInfo(interfaceMethodParameter(parameter, RequestHeader.class));
            }
        });

        list.add(0, new ServletCookieValueMethodArgumentResolver(beanFactory) {  // CookieValue 支持接口注解
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, CookieValue.class));
            }

            @Override
            protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
                return super.createNamedValueInfo(interfaceMethodParameter(parameter, CookieValue.class));
            }
        });

        list.add(0, new RequestResponseBodyMethodProcessor(adapter.getMessageConverters()) {    // RequestBody 支持接口注解
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, RequestBody.class));
            }

            @Override
            protected void validateIfApplicable(WebDataBinder binder, MethodParameter methodParam) {    // 支持@Valid验证
                super.validateIfApplicable(binder, interfaceMethodParameter(methodParam, Valid.class));
            }
        });

        // 修改ArgumentResolvers, 支持接口注解
        adapter.setArgumentResolvers(list);
    }
}
