package cn.springcloud.feign.sample.consumer.service;

import cn.springcloud.feign.sample.provider.service.ProviderService;
import cn.springcloud.feign.sample.provider.vo.Pojo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class TestService {

    @Autowired
    private ProviderService providerService;


    @GetMapping("/test")
    public String test() {
        Pojo pojo = new Pojo();
        pojo.setName("charles");
        pojo.setAge(18);
        pojo.setBirthday(new Date());
        return "Test " + providerService.hello(pojo);
    }

}
