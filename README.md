## venus-cloud-feign
  venus-cloud-feign,对Spring Cloud Feign的实战增强
  
  >如果你觉得venus-cloud-feign不错，让你很爽，烦请拨冗**“Star”**。
 
## Release Note

| 版本 | spring boot版本 | spring cloud 版本 |
| --- | --- | --- |
| 1.0.0  | 2.0.x.RELEASE | Finchley.RELEASE |

 
## 项目开发规范
 ### 包名规范
    cn.springcloud.feign
## 使用
目前已经发布到Maven中央仓库：
```
<dependency>
    <groupId>cn.springcloud.feign</groupId>
    <artifactId>venus-cloud-starter-feign</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 应用场景
主要由于使用了API(SDK)为了偷懒，以及Restful API路径中的版本带来的一系列问题。  

### spring MVC 不支持继承接口中方法参数上的注解（支持继承类、方法上的注解）
API中为了方便，使用feign替代RestTemplate手动调用。带来的问题：springMVC注解想偷懒，只在feign接口写一遍，然后实现类继承此接口即可。
例如：
feign接口定义如下  

    @FeignClient(ProviderApiAutoConfig.PLACE_HOLD_SERVICE_NAME)
    public interface ProductService {
        // 为了让spring mvc能够正确绑定变量
        public class Page extends PageRequest<Product> {
        }
        @RequestMapping(value = "/{version}/pt/product", method = RequestMethod.POST)
        Response<Product> insert(@RequestBody Product product);
    }

service实现类方法参数必须再写一次@RequestBody注解，方法上的@RequestMapping注解可以省略  

    @RestController
    public class ProductServiceImpl implements ProductService {
        @Override
        public Response<Product> insert(@RequestBody Product product) {
            product.setId(1L);
            return new Response(product);
        }
    }

解决办法，@Configuration配置类添加如下代码，扩展spring默认的ArgumentResolvers  

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

### swagger不支持继承接口中方法参数上的注解（支持继承类、方法上的注解）
没有找到swagger自带扩展点能够优雅扩展的方法，只好修改源码了，下载springfox-spring-web 2.8.0 release源码包。
添加pom.xml  

    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <groupId>io.springfox</groupId>
        <artifactId>springfox-spring-web</artifactId>
        <version>2.8.0-charles</version>
        <packaging>jar</packaging>
    
        <properties>
            <java.version>1.8</java.version>
            <resource.delimiter>@</resource.delimiter>
            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
            <maven.compiler.source>${java.version}</maven.compiler.source>
            <maven.compiler.target>${java.version}</maven.compiler.target>
        </properties>
    
        <dependencyManagement>
            <dependencies>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-dependencies</artifactId>
                    <!--<version>2.0.0.RELEASE</version>-->
                    <version>1.5.10.RELEASE</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
            </dependencies>
        </dependencyManagement>
    
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
    
            <dependency>
                <groupId>org.reflections</groupId>
                <artifactId>reflections</artifactId>
                <version>0.9.11</version>
            </dependency>
    
            <!-- swagger -->
            <dependency>
                <groupId>io.springfox</groupId>
                <artifactId>springfox-swagger2</artifactId>
                <version>2.8.0</version>
                <exclusions>
                    <exclusion>
                        <groupId>io.springfox</groupId>
                        <artifactId>springfox-spring-web</artifactId>
                    </exclusion>
                </exclusions>
            </dependency>
        </dependencies>
    </project>

添加ResolvedMethodParameterInterface继承ResolvedMethodParameter  

    public class ResolvedMethodParameterInterface extends ResolvedMethodParameter {
        public ResolvedMethodParameterInterface(String paramName, MethodParameter methodParameter, ResolvedType parameterType) {
            this(methodParameter.getParameterIndex(),
                    paramName,
                    interfaceAnnotations(methodParameter),
                    parameterType);
        }
    
        public ResolvedMethodParameterInterface(int parameterIndex, String defaultName, List<Annotation> annotations, ResolvedType parameterType) {
            super(parameterIndex, defaultName, annotations, parameterType);
        }
    
        public static List<Annotation> interfaceAnnotations(MethodParameter methodParameter) {
            List<Annotation> annotationList = new ArrayList<>();
            annotationList.addAll(Arrays.asList(methodParameter.getParameterAnnotations()));
    
            if (CollectionUtils.isEmpty(annotationList)) {
                for (Class<?> itf : methodParameter.getDeclaringClass().getInterfaces()) {
                    try {
                        Method method = itf.getMethod(methodParameter.getMethod().getName(), methodParameter.getMethod().getParameterTypes());
                        MethodParameter itfParameter = new MethodParameter(method, methodParameter.getParameterIndex());
                        annotationList.addAll(Arrays.asList(itfParameter.getParameterAnnotations()));
                    } catch (NoSuchMethodException e) {
                        continue;
                    }
                }
            }
    
            return annotationList;
        }
    }
    
修改HandlerMethodResolver类line 181，将ResolvedMethodParameter替换为ResolvedMethodParameterInterface，重新打包deploy，并在swagger相关依赖中强制指定修改后的版本。

    <!-- swagger -->
    <dependency>
        <groupId>io.springfox</groupId>
        <artifactId>springfox-swagger2</artifactId>
    </dependency>
    <dependency>
        <groupId>io.springfox</groupId>
        <artifactId>springfox-swagger-ui</artifactId>
    </dependency>
    <!--扩展swagger支持从接口获得方法参数注解-->
    <dependency>
        <groupId>io.springfox</groupId>
        <artifactId>springfox-spring-web</artifactId>
        <version>2.8.0-charles</version>
    </dependency>

这样就能够顺利生产swagger文档啦。

### feign不支持GET方法传递POJO
由于springMVC是支持GET方法直接绑定POJO的，只是feign实现并未覆盖所有springMVC特效，网上的很多变通方法都不是很好，要么是吧POJO拆散成一个一个单独的属性放在方法参数里，要么是把方法参数变成Map，要么就是要违反Restful规范，GET传递@RequestBody：  
https://www.jianshu.com/p/7ce46c0ebe9d  
https://github.com/spring-cloud/spring-cloud-netflix/issues/1253  
解决办法，使用feign拦截器：

    public class CharlesRequestInterceptor implements RequestInterceptor {
        @Autowired
        private ObjectMapper objectMapper;
    
        @Override
        public void apply(RequestTemplate template) {
            // feign 不支持 GET 方法传 POJO, json body转query
            if (template.method().equals("GET") && template.body() != null) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(template.body());
                    template.body(null);
    
                    Map<String, Collection<String>> queries = new HashMap<>();
                    buildQuery(jsonNode, "", queries);
                    template.queries(queries);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    
        private void buildQuery(JsonNode jsonNode, String path, Map<String, Collection<String>> queries) {
            if (!jsonNode.isContainerNode()) {   // 叶子节点
                if (jsonNode.isNull()) {
                    return;
                }
                Collection<String> values = queries.get(path);
                if (null == values) {
                    values = new ArrayList<>();
                    queries.put(path, values);
                }
                values.add(jsonNode.asText());
                return;
            }
            if (jsonNode.isArray()) {   // 数组节点
                Iterator<JsonNode> it = jsonNode.elements();
                while (it.hasNext()) {
                    buildQuery(it.next(), path, queries);
                }
            } else {
                Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    if (StringUtils.hasText(path)) {
                        buildQuery(entry.getValue(), path + "." + entry.getKey(), queries);
                    } else {  // 根节点
                        buildQuery(entry.getValue(), entry.getKey(), queries);
                    }
                }
            }
        }
    }
    
### feign不支持路径中的{version}
对于一个典型的Restful API定义如下：

    @ApiOperation("带过滤条件和排序的分页查询")
    @RequestMapping(value = "/{version}/pb/product", method = RequestMethod.GET)
    // 当前版本新开发api 随微服务整体升级 pt=protected 受保护的网关token验证合法可调用
    @ApiImplicitParam(name = "version", paramType = "path", allowableValues = ProviderApiAutoConfig.CURRENT_VERSION, required = true)
    Response<PageData<Product, Product>> selectAllGet(Page page);
    
我们并不关心路径中的{version}，因此方法参数中也没有@PathVariable("version")，这个时候feign就傻了，不知道路径中的{version}应该被替换成什么值。
解决办法 使用自己的Contract替换SpringMvcContract，先将SpringMvcContract代码复制过来，修改其中processAnnotationOnMethod方法的代码，从swagger注解中获得{version}的值：

    public class CharlesSpringMvcContract extends Contract.BaseContract
            implements ResourceLoaderAware {
        @Override
        protected void processAnnotationOnMethod(MethodMetadata data,
                                                 Annotation methodAnnotation, Method method) {
            if (!RequestMapping.class.isInstance(methodAnnotation) && !methodAnnotation
                    .annotationType().isAnnotationPresent(RequestMapping.class)) {
                return;
            }
    
            RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            // HTTP Method
            RequestMethod[] methods = methodMapping.method();
            if (methods.length == 0) {
                methods = new RequestMethod[]{RequestMethod.GET};
            }
            checkOne(method, methods, "method");
            data.template().method(methods[0].name());
    
            // path
            checkAtMostOne(method, methodMapping.value(), "value");
            if (methodMapping.value().length > 0) {
                String pathValue = Util.emptyToNull(methodMapping.value()[0]);
                if (pathValue != null) {
                    pathValue = resolve(pathValue);
                    // Append path from @RequestMapping if value is present on method
                    if (!pathValue.startsWith("/")
                            && !data.template().toString().endsWith("/")) {
                        pathValue = "/" + pathValue;
                    }
                    // 处理version
                    if (pathValue.contains("/{version}/")) {
                        Set<ApiImplicitParam> apiImplicitParams = AnnotatedElementUtils.findAllMergedAnnotations(method, ApiImplicitParam.class);
                        for (ApiImplicitParam apiImplicitParam : apiImplicitParams) {
                            if ("version".equals(apiImplicitParam.name())) {
                                String version = apiImplicitParam.allowableValues().split(",")[0].trim();
                                pathValue = pathValue.replaceFirst("\\{version\\}", version);
                            }
                        }
                    }
                    data.template().append(pathValue);
                }
            }
    
            // produces
            parseProduces(data, method, methodMapping);
    
            // consumes
            parseConsumes(data, method, methodMapping);
    
            // headers
            parseHeaders(data, method, methodMapping);
    
            data.indexToExpander(new LinkedHashMap<Integer, Param.Expander>());
        }
    }

然后在自己的AutoConfig中声明成spring的bean  

    @Configuration
    @ConditionalOnClass(Feign.class)
    public class FeignAutoConfig {
        @Bean
        public Contract charlesSpringMvcContract(ConversionService conversionService) {
            return new CharlesSpringMvcContract(Collections.emptyList(), conversionService);
        }
    
        @Bean
        public CharlesRequestInterceptor charlesRequestInterceptor(){
            return new CharlesRequestInterceptor();
        }
    }

