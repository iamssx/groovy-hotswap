package com.ssx.groovyhotswap;

import com.ssx.groovyhotswap.service.GroovyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroovyController {

    @Autowired
    private GroovyService groovyService;

    @GetMapping("/")
    public Object index() {
        return groovyService.hotswap();
    }
}
