package cn.springcloud.feign.sample.provider.service;

import cn.springcloud.feign.sample.provider.vo.Pojo;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProviderServiceImpl implements ProviderService {
    @Override
    public String hello(Long id, Pojo pojo) {
        return "Hello " + pojo.getName();
    }
}
