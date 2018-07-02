package cn.springcloud.feign.sample.provider.service;

import cn.springcloud.feign.sample.provider.vo.Pojo;
import io.swagger.annotations.ApiImplicitParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("provider")
public interface ProviderService {
    @ApiImplicitParam(name = "version", paramType = "path", allowableValues = "v2,v1", required = true)
    @GetMapping("/{version}/hello/{id:\\d+}")
    String hello(@PathVariable("id") Long id, @RequestParam Pojo pojo);
}
