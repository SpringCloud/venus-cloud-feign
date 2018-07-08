package cn.springcloud.feign.sample.provider.service;

import cn.springcloud.feign.sample.provider.vo.Pojo;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class ProviderServiceImpl implements ProviderService {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ"), true));
    }

    @Override
    public String hello(Pojo pojo) {
        return "Hello " + pojo.getName();
    }
}
